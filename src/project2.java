//----------------------------------------------------------------------------------------
/* Name: Andrew Fugate
 * Course: CNT 4714 Fall 2024 
 * Assignment title: Project 2 – Synchronized/Cooperating Threads – A Banking Simulation 
 * Due Date: September 22, 2024
 */
//----------------------------------------------------------------------------------------
import java.util.concurrent.locks.*;
import java.util.concurrent.*;
import java.util.Random;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
//----------------------------------------------------------------------------------------
public class project2
{
    public static void main( String[] args )
    {   
    	
    	try
    	{
    		FileOutputStream consoleFile = new FileOutputStream( "ConsoleOutputOfSim.txt" );
    		redir red = new redir( System.out, new PrintStream( consoleFile ) );
    		System.setOut( red );
    	}
    	
    	catch( FileNotFoundException e )
    	{
    		System.out.println( "Error in console redirect file" );
    	}
    	
    	TransactionNumberMethod TransactionNumber = new TransactionNumberMethod( "" );
    	
    	//create accounts
        BankAccount account1 = new BankAccount( "account 1" , 0, TransactionNumber );
        BankAccount account2 = new BankAccount( "account 2" , 0, TransactionNumber );
        BankAccount[] accounts = { account1, account2 };

        //19 threads from 5 depositors, 10 withdraw, 2 transfers, 1 audit, 1 treasury
        ExecutorService executor = Executors.newFixedThreadPool( 19 );
      
        Random random = new Random();
        
        //sleep time defined here for convienence
        //more is longer
        //fastest to slowest: with - depo - transfer - internal - IRS
        int WithdrawalAgentSleepTime = 100;
        int DepositorAgentSleepTime = 1000;
        int TransferAgentSleepTime = 5000;
        int InternalAuditAgentSleepTime = 10000;
        int TreasuryAgentSleepTime = 10500;
        
        System.out.println( "\t\t\t\t* * * SIMULATION BEGINNING * * *\n" );
        System.out.println( "Deposit Agents:\t\tWithdrawal Agents:\t\t\tBalances:\t\tTransaction Number:" );
        System.out.println( "_______________\t\t__________________\t\t\t_________\t\t___________________" );

        //start thread of depositor with random account
        for( int i = 0; i < 5; i++ )
        {
        	executor.execute( new DepositorAgent( accounts, i+1, DepositorAgentSleepTime ) );
        }
        
        for( int i = 0; i < 10; i++ )
        {
        	executor.execute( new WithdrawalAgent( accounts, i+1, WithdrawalAgentSleepTime ) );
        }
        
        for( int i = 0; i < 2; i++ )
        {
            executor.execute( new TransferAgent( accounts, i+1, TransferAgentSleepTime ) );
        }
        
        executor.execute( new InternalAuditAgent ( accounts, 18, InternalAuditAgentSleepTime ) );
        
        executor.execute( new TreasuryAgent ( accounts, 19, TreasuryAgentSleepTime ) );

        executor.shutdown();
    }
}
//----------------------------------------------------------------------------------------
class redir extends PrintStream
{
	PrintStream redir;
	
	public redir( PrintStream sys, PrintStream file )
	{
		super( sys );
		redir = file;
	}
	
	@Override
	public void println( String out )
	{
		super.println( out );
		redir.println(out);
	}
	
	@Override
	public void close()
	{
		super.close();
		redir.close();
	}
}
//----------------------------------------------------------------------------------------
class DepositorAgent implements Runnable
{
	int ThreadName;
	int DepoSleep;
	BankAccount[] accountArray;
	private Random rand = new Random();
	
	CSVwrite flagCSV = new CSVwrite();
	
	public DepositorAgent( BankAccount[] accounts, int threadNumber, int sleepTime )
	{
		this.accountArray = accounts;
		this.ThreadName = threadNumber;
		this.DepoSleep = sleepTime;
	}

	@Override
	public void run()
	{
		while( true )
		{
			BankAccount account = accountArray[ rand.nextInt( accountArray.length ) ];

			int depositAmount = rand.nextInt(600) + 1;
			
			//WILL WAIT TF HERE UNITL LOCK AVAIL
			account.AccountLock.lock();
			
			try
			{
				account.deposit( depositAmount );
			}
			
			finally
			{
				int transNum = account.TransactionCounter.TransactionNumberMethod();
				
				System.out.println( "Agent DT" + ThreadName + " deposits $" + depositAmount + " into: JA-" + account.accountNumber.substring( account.accountNumber.length() - 1)
									+ "\t\t\t(+) JA-" + account.accountNumber.substring( account.accountNumber.length() - 1) + " balance is $" + account.balance
									+ "  \t\t" + transNum );
				
				if( depositAmount >= 450 )
				{
					flagCSV.flagCSV( "Agent DT", ThreadName, "deposit", depositAmount, transNum );
				}
				
				account.sufficientAmountForWithdraw.signalAll();
				account.AccountLock.unlock();
			}
            
            try
            {
                Thread.sleep( rand.nextInt( DepoSleep ) + 1 );  // Random sleep time
            }
            
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
		}
	}
}
//----------------------------------------------------------------------------------------
class WithdrawalAgent implements Runnable
{
	int ThreadName;
	int WithSleep;
	BankAccount[] accountArray;
	private Random rand = new Random();
	
	CSVwrite flagCSV = new CSVwrite();
	
	public WithdrawalAgent( BankAccount[] accounts, int threadNumber, int sleepTime )
	{
		this.accountArray = accounts;
		this.ThreadName = threadNumber;
		this.WithSleep = sleepTime;
	}

	@Override
	public void run()
	{
		while( true )
		{
			BankAccount account = accountArray[ rand.nextInt( accountArray.length ) ];

			int withdrawAmount = rand.nextInt(98) + 1;
			
			//WILL WAIT TF HERE UNITL LOCK AVAIL
			account.AccountLock.lock();
			
			try
			{
				while( account.balance < withdrawAmount )
				{
					try
					{
						//System.out.println("not enough money to take");
						
						System.out.println( "\t\tAgent WT" + ThreadName + " attempts to withdraw $" + withdrawAmount + " from JA-" + account.accountNumber.substring( account.accountNumber.length() - 1)
						+ "\t****** WITHDRAWAL BLOCKED ******" 
						+ "\n\t\t\t\t\t\t\t\t      !INSUFFICIENT FUNDS!"
						+ "\n\t\t\t\t\t\t\t\t\tBalance only $" + account.balance + "\n" );
						
						account.sufficientAmountForWithdraw.await();
					}
					
					catch( InterruptedException e )
					{
						e.printStackTrace();
					}
				}
				
				account.withdraw( withdrawAmount );
			}
			
			finally
			{
				int transNum = account.TransactionCounter.TransactionNumberMethod();
				
				System.out.println( "\t\tAgent WT" + ThreadName + " withdraws $" + withdrawAmount + " from JA-" + account.accountNumber.substring( account.accountNumber.length() - 1)
									+ "\t(-) JA-" + account.accountNumber.substring( account.accountNumber.length() - 1) + " balance is $" + account.balance
									+ "  \t\t" + transNum );
				
				if( withdrawAmount >= 90 )
				{
					flagCSV.flagCSV( "Agent WT", ThreadName, "withdrawal", withdrawAmount, transNum );
				}
				
				account.AccountLock.unlock();
			}
            
            try
            {
                Thread.sleep(rand.nextInt( WithSleep ) + 1 );  // Random sleep time
            }
            
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
		}
	}
}
//----------------------------------------------------------------------------------------
class TransferAgent implements Runnable
{
	int ThreadName;
	int TransSleep;
	BankAccount[] accountArray;
	private Random rand = new Random();
	
	public TransferAgent( BankAccount[] accounts, int threadNumber, int sleepTime )
	{
		this.accountArray = accounts;
		this.ThreadName = threadNumber;
		this.TransSleep = sleepTime;
	}

	@Override
	public void run()
	{
		while( true )
		{
			BankAccount accountTo = accountArray[ rand.nextInt( accountArray.length ) ];
			BankAccount accountFrom = accountArray[0];
			
			if( accountFrom.equals( accountTo ) )
			{
				accountFrom = accountArray[1];
			}

			int transferAmount = rand.nextInt(98) + 1;
			
			boolean lockAccountTo = false;
            boolean lockAccountFrom = false;
			
            try
            {
                // Try to lock both accounts, no blocking
                lockAccountTo = accountTo.AccountLock.tryLock();
                lockAccountFrom = accountFrom.AccountLock.tryLock();

                if( lockAccountTo && lockAccountFrom )
                {
                	if( accountFrom.balance < transferAmount )
        			{
                		System.out.println( "\nTRANSFER-->Agent TR" + ThreadName + " attempts to transfer $" + transferAmount + " from JA-" + accountFrom.accountNumber.substring( accountFrom.accountNumber.length() - 1)
    					+ "\n******TRANSFER BLOCKED******\n     INSUFFICIENT FUNDS\n  JA-" + accountFrom.accountNumber.substring( accountFrom.accountNumber.length() - 1) + " balance is only $" + accountFrom.balance + "\n" );
        				
        			}
        			
        			else
        			{
        				accountFrom.balance = accountFrom.balance - transferAmount;
        				accountTo.balance = accountTo.balance + transferAmount;
        				
        				//System.out.println("Transfer of $" + transferAmount + " from " + accountFrom.accountNumber + " to " + accountTo.accountNumber + " in transaction " + accountTo.TransactionCounter.TransactionNumberMethod() );
        				
        				System.out.println( "\nTRANSFER-->Agent TR" + ThreadName + " transferring $" + transferAmount + " from JA-" + accountFrom.accountNumber.substring( accountFrom.accountNumber.length() - 1)
        																											  + " to JA-" 	+ accountTo.accountNumber.substring( accountTo.accountNumber.length() - 1)
											+ " --> JA-" + accountFrom.accountNumber.substring( accountFrom.accountNumber.length() - 1) + " balance is now $" + accountFrom.balance + "\t\t" + + accountTo.TransactionCounter.TransactionNumberMethod()
											+ "\nTRANSFER COMPLETE\t\t\t\t\t   JA-" + accountTo.accountNumber.substring( accountTo.accountNumber.length() - 1) + " balance is now $" + accountTo.balance + "\n" );
        			}
                }
                
                else
                {
                	//System.out.println("Trying Transfer Later - NO MONEYS BROKE BOI");
                	if( !lockAccountTo )
                	{
                		System.out.println("******TRANSFER ABORTED******\t JA-" + accountTo.accountNumber.substring( accountTo.accountNumber.length() - 1) + " is unavailable" );
                	}
                	
                	else
                	{
                		System.out.println("******TRANSFER ABORTED******\t JA-" + accountFrom.accountNumber.substring( accountFrom.accountNumber.length() - 1) + " is unavailable" );
                	}
                }
            }
            
            finally
            {
	        	if( lockAccountTo )
	        	{
	        		accountTo.AccountLock.unlock();
	        	}
	        	
	        	if( lockAccountFrom )
	        	{
	        		accountFrom.AccountLock.unlock();
	        	}
            }
				
            try
            {
                Thread.sleep(rand.nextInt( TransSleep ) + 1 );  // Random sleep time
            }
            
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
		}
	}
}
//----------------------------------------------------------------------------------------
class InternalAuditAgent implements Runnable
{
	int ThreadName;
	int IntAuditSleep;
	BankAccount[] accountArray;
	private Random rand = new Random();
	
	public InternalAuditAgent( BankAccount[] accounts, int threadNumber, int sleepTime )
	{
		this.accountArray = accounts;
		this.ThreadName = threadNumber;
		this.IntAuditSleep = sleepTime;
	}
	
	@Override
	public void run()
	{
		while( true )
		{
			BankAccount account1 = accountArray[ 0 ];
			BankAccount account2 = accountArray[ 1 ];
			
			//account1.TransactionCounter.BankAuditCall();
			
			boolean lockAccount1 = false;
            boolean lockAccount2 = false;
			
            try
            {
                lockAccount1 = account1.AccountLock.tryLock();
                lockAccount2 = account2.AccountLock.tryLock();

                if( lockAccount1 && lockAccount2 )
                {
                	System.out.println("**************************************************************************"
                						+ "\nInternal Bank Audit Beginning...\n"
                						+ "\tThe total number of transactions since last Internal Audit is: " + account1.TransactionCounter.BankAuditDiff()
                						+ "\n\n\tINTERNAL BANK AUDITOR FINDS CURRENT ACCOUNT BALANCE FOR JA-1 TO BE: " + account1.balance
                						+ "\n\tINTERNAL BANK AUDITOR FINDS CURRENT ACCOUNT BALANCE FOR JA-2 TO BE: " + account2.balance
                						+ "\n\n\nInternal Bank Audit Complete.\n"
                						+ "**************************************************************************");
                	
                	account1.TransactionCounter.BankAuditCall();
                }
                
                else
                {
                	System.out.println("**************************************************************************"
                					+  "\nAudit Failed - try again later\n"
                					+  "**************************************************************************\n");
                }
            }
            
            finally
            {
	        	if( lockAccount1 )
	        	{
	        		account1.AccountLock.unlock();
	        	}
	        	
	        	if( lockAccount2 )
	        	{
	        		account2.AccountLock.unlock();
	        	}
            }
				
            try
            {
                Thread.sleep(rand.nextInt( IntAuditSleep ) + 1 );  // Random sleep time
            }
            
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
		}
	}
}
//----------------------------------------------------------------------------------------
class TreasuryAgent implements Runnable
{
	int ThreadName;
	int TreasSleep;
	BankAccount[] accountArray;
	private Random rand = new Random();
	
	public TreasuryAgent( BankAccount[] accounts, int threadNumber, int sleepTime )
	{
		this.accountArray = accounts;
		this.ThreadName = threadNumber;
		this.TreasSleep = sleepTime;
	}
	
	@Override
	public void run()
	{
		while( true )
		{
			BankAccount account1 = accountArray[ 0 ];
			BankAccount account2 = accountArray[ 1 ];
			
			boolean lockAccount1 = false;
            boolean lockAccount2 = false;
			
            try
            {
                lockAccount1 = account1.AccountLock.tryLock();
                lockAccount2 = account2.AccountLock.tryLock();

                if( lockAccount1 && lockAccount2 )
                {
                	System.out.println("***IRS Audit***\tCurrent Balances: " + account1.accountNumber + ": $" + account1.balance + " - " + account2.accountNumber + ": $" + account2.balance );
                }
                
                else
                {
                	System.out.println("Audit Failed - We're onto you like Capone!");
                }
            }
            
            finally
            {
	        	if( lockAccount1 )
	        	{
	        		account1.AccountLock.unlock();
	        	}
	        	
	        	if( lockAccount2 )
	        	{
	        		account2.AccountLock.unlock();
	        	}
            }
				
            try
            {
                Thread.sleep(rand.nextInt( TreasSleep ) + 1 );  // Random sleep time
            }
            
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
		}
	}
}
//----------------------------------------------------------------------------------------
class BankAccount
{
	String accountNumber;
	TransactionNumberMethod TransactionCounter;
	
	int balance;
	
	ReentrantLock AccountLock = new ReentrantLock();
	Condition sufficientAmountForWithdraw = AccountLock.newCondition();
	
    public BankAccount( String name, int initBal, TransactionNumberMethod trs )
    {
		this.accountNumber = name;
		this.balance = initBal;
		this.TransactionCounter = trs;
	}

	public void withdraw( int withdraw )
	{
		this.balance = this.balance - withdraw;	
		//System.out.println( "Withdraw of $" + withdraw + " in account " + accountNumber + " in transaction " + TransactionCounter.TransactionNumberMethod()  );
	}

	public void deposit( int deposit )
	{
		this.balance = this.balance + deposit;
		//System.out.println( "Deposit of $" + deposit + " in account " + accountNumber + " in transaction " + TransactionCounter.TransactionNumberMethod()  );
	} 
}
//----------------------------------------------------------------------------------------
class TransactionNumberMethod
{
	int transactionNumber = 1;
	
	int BankAuditLastTrans = 0;
	
	public TransactionNumberMethod( String init )
	{
		//init the counter without increment
		transactionNumber = transactionNumber - 1;
	}

	public int BankAuditDiff()
	{
		return transactionNumber - BankAuditLastTrans;
	}

	public void BankAuditCall()
	{
		BankAuditLastTrans = transactionNumber;
	}

	public int TransactionNumberMethod()
	{
		transactionNumber++;
		
		return transactionNumber;
	}
}
//----------------------------------------------------------------------------------------
class CSVwrite
{
	void flagCSV( String Agent, int agentNum, String typeOf, int amount, int transNum )
	{
		String CSVout = null;
		String date = null;
	
		System.out.println( "***Flagged Transaction***" + Agent + agentNum + " made a " + typeOf + " in excess of $" + amount + ".00 USD - See Flagged Transaction Log." );
		
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern( "MM/dd/yyyy HH:mm:ss z" );
        date = now.atZone(ZoneId.systemDefault()).format(outputFormatter);
        
		CSVout = Agent + agentNum + " issued " + typeOf + "\t of $" + amount + ".00\tat:" + date + "\tTransaction Number : " + transNum;
		
		try( FileWriter writer = new FileWriter( "transactions.csv", true ) )
		{
	        writer.write( CSVout + "\n" );
        }
		
		catch( IOException e )
		{
            e.printStackTrace();
        }
	}
}
//----------------------------------------------------------------------------------------