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
        BankAccount account1 = new BankAccount();
        BankAccount account2 = new BankAccount();
        BankAccount[] accounts = { account1, account2 };

        ExecutorService executor = Executors.newFixedThreadPool( 19 );

        int transactionNumber = 1;
        Random random = new Random();

        //start thread of depositor with random account
        for( int i = 0; i < 5; i++ )
        {
        	//executor.execute( new DepositorAgent( accounts[ random.nextInt( accounts.length ) ], transactionNumber ) );
        }
        
        for( int i = 0; i < 10; i++ )
        {
        	//executor.execute( new WithdrawalAgent( accounts[ random.nextInt( accounts.length ) ], transactionNumber ) );
        }
        
        for (int i = 0; i < 2; i++)
        {
            //executor.execute(new TransferAgent(accounts[0], accounts[1], transactionNumber));
        }
        
        //executor.execute( new InternalAuditAgent ( accounts ) );
        
        //executor.execute( new TreasuryAgent ( accounts ) );

        //executor.shutdown();
    }
}
//----------------------------------------------------------------------------------------
class DepositorAgent implements Runnable
{
	@Override
	public void run()
	{
		//
	}
}
//----------------------------------------------------------------------------------------
class WithdrawalAgent implements Runnable
{
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
    //
}
//----------------------------------------------------------------------------------------

//----------------------------------------------------------------------------------------

//----------------------------------------------------------------------------------------

//----------------------------------------------------------------------------------------
