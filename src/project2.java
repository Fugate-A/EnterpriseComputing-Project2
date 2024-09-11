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
    public static void main(String[] args)
    {
        BankAccount account1 = new BankAccount();
        BankAccount account2 = new BankAccount();
        BankAccount[] accounts = { account1, account2 };

        ExecutorService executor = Executors.newFixedThreadPool( 19 );

        int transactionNumber = 1;

        // Start agents
        for (int i = 0; i < 5; i++)
        {
            executor.execute(new DepositorAgent(accounts[i % 2], transactionNumber));
        }
        
        for (int i = 0; i < 10; i++)
        {
            executor.execute(new WithdrawalAgent(accounts[i % 2], transactionNumber));
        }
        
        for (int i = 0; i < 2; i++)
        {
            executor.execute(new TransferAgent(accounts[0], accounts[1], transactionNumber));
        }
        
        executor.execute(new InternalAuditAgent(accounts));
        executor.execute(new TreasuryAgent(accounts));

        executor.shutdown();
    }
}
//----------------------------------------------------------------------------------------
class DepositorAgent implements Runnable
{
    private BankAccount account;
    private int transactionNumber;
    private Random rand = new Random();

    public DepositorAgent(BankAccount account, int transactionNumber)
    {
        this.account = account;
        this.transactionNumber = transactionNumber;
    }

    @Override
    public void run()
    {
        while (true)
        {
            int depositAmount = rand.nextInt(600) + 1;
            
            try
            {
                account.deposit(depositAmount, transactionNumber++);
            }
            
            catch (IOException e)
            {
                e.printStackTrace();
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
    private BankAccount account;
    private int transactionNumber;
    private Random rand = new Random();

    public WithdrawalAgent(BankAccount account, int transactionNumber)
    {
        this.account = account;
        this.transactionNumber = transactionNumber;
    }

    @Override
    public void run()
    {
        while (true)
        {
            int withdrawAmount = rand.nextInt(99) + 1;
            
            try
            {
                boolean success = account.withdraw(withdrawAmount, transactionNumber++);
                while (!success) {
                    Thread.sleep(500);  // Wait until balance is sufficient
                    success = account.withdraw(withdrawAmount, transactionNumber++);
                }
            }
            
            catch (IOException | InterruptedException e)
            {
                e.printStackTrace();
            }
            
            try
            {
                Thread.sleep(rand.nextInt(500));  // Random sleep time
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
    private BankAccount fromAccount;
    private BankAccount toAccount;
    private int transactionNumber;
    private Random rand = new Random();
    private final ReentrantLock lock = new ReentrantLock();

    public TransferAgent(BankAccount fromAccount, BankAccount toAccount, int transactionNumber)
    {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.transactionNumber = transactionNumber;
    }

    @Override
    public void run()
    {
        while (true)
        {
            lock.lock();
            
            try
            {
                int transferAmount = rand.nextInt(99) + 1;
                
                if (fromAccount.withdraw(transferAmount, transactionNumber))
                {
                    toAccount.deposit(transferAmount, transactionNumber++);
                }
            }
            
            catch (IOException e)
            {
                e.printStackTrace();
            }
            
            finally
            {
                lock.unlock();
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
    private BankAccount[] accounts;
    private int auditCount = 0;

    public InternalAuditAgent(BankAccount[] accounts)
    {
        this.accounts = accounts;
    }

    @Override
    public void run()
    {
        while (true) 
        {
            for (BankAccount account : accounts)
            {
                account.lock.lock();
            }
            
            try
            {
                System.out.println("Internal Audit: Account 1 Balance: $" + accounts[0].getBalance() +
                        " | Account 2 Balance: $" + accounts[1].getBalance() +
                        " | Audit Count: " + auditCount++);
            }
            
            finally
            {
                for (BankAccount account : accounts)
                {
                    account.lock.unlock();
                }
            }
            
            try
            {
                Thread.sleep(3000);  // Longer sleep time for auditors
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
    private BankAccount[] accounts;
    private int treasuryCount = 0;

    public TreasuryAgent(BankAccount[] accounts)
    {
        this.accounts = accounts;
    }

    @Override
    public void run() {
        while (true) {
            for (BankAccount account : accounts) {
                account.lock.lock();
            }
            try {
                System.out.println("Treasury Audit: Account 1 Balance: $" + accounts[0].getBalance() +
                        " | Account 2 Balance: $" + accounts[1].getBalance() +
                        " | Treasury Count: " + treasuryCount++);
            } finally {
                for (BankAccount account : accounts) {
                    account.lock.unlock();
                }
            }
            try {
                Thread.sleep(5000);  // Longer sleep time for treasury
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
//----------------------------------------------------------------------------------------
class BankAccount
{
    private int balance;
    final ReentrantLock lock = new ReentrantLock();
    
    public BankAccount()
    {
        this.balance = 0;
    }

    public int getBalance()
    {
        return balance;
    }

    public void deposit(int amount, int transactionNumber) throws IOException
    {
        lock.lock();
        
        try
        {
            balance += amount;
            logTransaction("Deposit", amount, balance, transactionNumber);
        }
        
        finally
        {
            lock.unlock();
        }
    }

    public boolean withdraw(int amount, int transactionNumber) throws IOException
    {
        lock.lock();
        
        try
        {
            if (balance >= amount)
            {
                balance -= amount;
                logTransaction("Withdraw", amount, balance, transactionNumber);
                return true;
            }
            
            return false;
        }
        
        finally
        {
            lock.unlock();
        }
    }

    private void logTransaction(String type, int amount, int newBalance, int transactionNumber) throws IOException
    {
        System.out.println(type + " of $" + amount + " | New Balance: $" + newBalance + " | Transaction #" + transactionNumber);
        
        if ((type.equals("Deposit") && amount > 450) || (type.equals("Withdraw") && amount > 90))
        {
            FileWriter writer = new FileWriter("transactions.csv", true);
            writer.write(transactionNumber + ", " + type + ", " + amount + ", " + newBalance + ", " + getTimeStamp() + "\n");
            writer.close();
        }
    }

    private String getTimeStamp()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        return sdf.format(new Date());
    }
}
//----------------------------------------------------------------------------------------

//----------------------------------------------------------------------------------------

//----------------------------------------------------------------------------------------

//----------------------------------------------------------------------------------------
