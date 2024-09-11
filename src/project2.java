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
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
//----------------------------------------------------------------------------------------
public class project2
{
    public static void main( String[] args )
    {   	
    	TransactionNumberMethod TransactionNumber = new TransactionNumberMethod( "" );
    	
    	//create accounts
        BankAccount account1 = new BankAccount( "account 1" , 0, TransactionNumber );
        BankAccount account2 = new BankAccount( "account 2" , 0, TransactionNumber );
        BankAccount[] accounts = { account1, account2 };

        //19 threads from 5 depositors, 10 withdraw, 2 transfers, 1 audit, 1 treasury
        ExecutorService executor = Executors.newFixedThreadPool( 19 );
      
        Random random = new Random();

        //start thread of depositor with random account
        for( int i = 0; i < 5; i++ )
        {
        	executor.execute( new DepositorAgent( accounts, i ) );
        }
        
        for( int i = 0; i < 10; i++ )
        {
        	executor.execute( new WithdrawalAgent( accounts, i ) );
        }
        
        for( int i = 0; i < 2; i++ )
        {
            executor.execute( new TransferAgent( accounts, i ) );
        }
        
        //executor.execute( new InternalAuditAgent ( accounts ) );
        
        //executor.execute( new TreasuryAgent ( accounts ) );

        //executor.shutdown();
    }
}
//----------------------------------------------------------------------------------------
class DepositorAgent implements Runnable
{
	int ThreadName;
	BankAccount[] accountArray;
	private Random rand = new Random();
	
	public DepositorAgent( BankAccount[] accounts, int threadNumber )
	{
		this.accountArray = accounts;
		this.ThreadName = threadNumber;
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
				account.sufficientAmountForWithdraw.signalAll();
				account.AccountLock.unlock();
			}
            
            try
            {
                Thread.sleep( rand.nextInt( 1000 ) );  // Random sleep time
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
	BankAccount[] accountArray;
	private Random rand = new Random();
	
	public WithdrawalAgent( BankAccount[] accounts, int threadNumber )
	{
		this.accountArray = accounts;
		this.ThreadName = threadNumber;
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
						System.out.println("not enough money to take");
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
				account.AccountLock.unlock();
			}
            
            try
            {
                Thread.sleep(rand.nextInt(1000));  // Random sleep time
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
	BankAccount[] accountArray;
	private Random rand = new Random();
	
	public TransferAgent( BankAccount[] accounts, int threadNumber )
	{
		this.accountArray = accounts;
		this.ThreadName = threadNumber;
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

			int transferAmount = rand.nextInt(988) + 1;
			
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
        				System.out.println("!!!!ABORTION!!!!");
        			}
        			
        			else
        			{
        				accountFrom.balance = accountFrom.balance - transferAmount;
        				accountTo.balance = accountTo.balance + transferAmount;
        				
        				System.out.println("Transfer of $" + transferAmount + " from " + accountFrom.accountNumber + " to " + accountTo.accountNumber + " in transaction " + accountTo.TransactionCounter.TransactionNumberMethod() );
        			}
                }
                
                else
                {
                	System.out.println("Trying Transfer Later - NO MONEYS BROKE BOI");
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
                Thread.sleep(rand.nextInt(1000));  // Random sleep time
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
	BankAccount[] accountArray;
	private Random rand = new Random();
	
	public InternalAuditAgent( BankAccount[] accounts, int threadNumber )
	{
		this.accountArray = accounts;
		this.ThreadName = threadNumber;
	}
	
	@Override
	public void run()
	{
		//
	}
}
//----------------------------------------------------------------------------------------
class TreasuryAgent implements Runnable
{
	int ThreadName;
	BankAccount[] accountArray;
	private Random rand = new Random();
	
	public TreasuryAgent( BankAccount[] accounts, int threadNumber )
	{
		this.accountArray = accounts;
		this.ThreadName = threadNumber;
	}
	
	@Override
	public void run()
	{
		//
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
		System.out.println( "Withdraw of $" + withdraw + " in account " + accountNumber + " in transaction " + TransactionCounter.TransactionNumberMethod()  );
	}

	public void deposit( int deposit )
	{
		this.balance = this.balance + deposit;
		System.out.println( "Deposit of $" + deposit + " in account " + accountNumber + " in transaction " + TransactionCounter.TransactionNumberMethod()  );
	} 
}
//----------------------------------------------------------------------------------------
class TransactionNumberMethod
{
	int transactionNumber = 0;
	
	public TransactionNumberMethod( String init )
	{
		//init the counter without increment
	}

	public int TransactionNumberMethod()
	{
		transactionNumber++;
		
		return transactionNumber;
	}
}
//----------------------------------------------------------------------------------------

//----------------------------------------------------------------------------------------