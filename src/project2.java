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
        	executor.execute( new DepositorAgent( accounts ) );
        }
        
        for( int i = 0; i < 10; i++ )
        {
        	executor.execute( new WithdrawalAgent( accounts[ random.nextInt( accounts.length ) ] ) );
        }
        
        /*for (int i = 0; i < 2; i++)
        {
            executor.execute( new TransferAgent( accounts[0], accounts[1] ) );
        }*/
        
        //executor.execute( new InternalAuditAgent ( accounts ) );
        
        //executor.execute( new TreasuryAgent ( accounts ) );

        //executor.shutdown();
    }
}
//----------------------------------------------------------------------------------------
class DepositorAgent implements Runnable
{
	BankAccount[] accountArray;
	private Random rand = new Random();
	
	public DepositorAgent( BankAccount[] accounts )
	{
		this.accountArray = accounts;
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
class WithdrawalAgent implements Runnable
{
	private Random rand = new Random();
	
	public WithdrawalAgent(BankAccount bankAccount)
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run()
	{
		//
	}
}
//----------------------------------------------------------------------------------------
class TransferAgent implements Runnable
{
	
	public TransferAgent( BankAccount account1, BankAccount account2 )
	{
		//
	}

	@Override
	public void run()
	{
		//
	}
}
//----------------------------------------------------------------------------------------
class InternalAuditAgent implements Runnable
{
	@Override
	public void run()
	{
		//
	}
}
//----------------------------------------------------------------------------------------
class TreasuryAgent implements Runnable
{
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
	
    public BankAccount( String name, int initBal, TransactionNumberMethod trs )
    {
		this.accountNumber = name;
		this.balance = initBal;
		this.TransactionCounter = trs;
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