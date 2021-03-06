package net.threescale.api.cache;

import net.threescale.api.CommonBase;
import net.threescale.api.LogFactory;
import net.threescale.api.v2.ApiTransaction;
import net.threescale.api.v2.ApiTransactionForAppId;
import net.threescale.api.v2.HttpSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;


public class ReportCachingTest extends CommonBase {

    Logger log = LogFactory.getLogger(this);

    protected ApiCache api_cache;

    @Mock
    protected HttpSender sender;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        api_cache = new DefaultCacheImpl(SERVER_URL, PROVIDER_KEY, sender);

    }


    @Test
    public void reportTransactions() throws Exception {

        api_cache.report(createTransactionData());

        ApiTransaction[] t1 = api_cache.getTransactionFor("bce4c8f4", "2009-01-01 14:23:08");
        assertNotNull("Transaction 1 was not stored in cache", t1);

        ApiTransaction[] t2 = api_cache.getTransactionFor("bad7e480", "2009-01-01 18:11:59");
        assertNotNull("Transaction 2 was not stored in cache", t2);
    }


    @Test
    public void isCorrectExpirationTimeSetForAppId() throws Exception {
        api_cache.report(createTransactionData());

        long time1 = api_cache.getTransactionExpirationTimeFor("bce4c8f4");
        assertEquals("T1 had wrong expiration time", api_cache.getCurrentResponseExpirationTime(), time1);
        long time2 = api_cache.getTransactionExpirationTimeFor("bad7e480");
        assertEquals("T2 had wrong expiration time", api_cache.getCurrentResponseExpirationTime(), time2);
    }


    @Test
    public void transactionAreExpiredAtCorrectTime() throws Exception {
        api_cache.setReportExpirationInterval(5L);
        api_cache.report(createTransactionData());
        Thread.sleep(1000L);

        ApiTransaction[] t1 = api_cache.getTransactionFor("bce4c8f4", "2009-01-01 14:23:08");
        assertNull("Transaction 1 was still in cache", t1);

        ApiTransaction[] t2 = api_cache.getTransactionFor("bad7e480", "2009-01-01 18:11:59");
        assertNull("Transaction 2 was was still in cache", t2);
    }


    @Test
    public void cacheEvictionTimeIsBeingSetCorrectly() throws Exception {
        api_cache.setReportExpirationInterval(200L);
        long currentTime = api_cache.getCurrentResponseExpirationTime();
        Thread.sleep(550L);
        long newTime = api_cache.getCurrentResponseExpirationTime();

        SimpleDateFormat df = new SimpleDateFormat("yyyy MM dd hh mm ss S");

        log.info("Current time: " + df.format(currentTime));
        log.info("    New time: " + df.format(newTime));
        assertTrue("ExpirationTime was not incremented correctly", newTime == (currentTime + 200L));
    }


    @Test
    public void evictedTransactionsAreSentToServer() throws Exception {
        api_cache.setReportExpirationInterval(5L);
        api_cache.report(createTransactionData());
        Thread.sleep(800L);

        verify(sender).sendPostToServer(SERVER_URL, RESPONSE_DATA);
    }

    @Test
    public void multipleReportsTransactionsAreSentToServerAsOneMessage() throws Exception {
        api_cache.setReportExpirationInterval(5L);
        api_cache.report(createTransactionData());
        api_cache.report(createSecondSetOfTransactionData());
        Thread.sleep(800L);

        verify(sender).sendPostToServer(SERVER_URL, MULTIPLE_RESPONSE_DATA);
    }

    @Test
    public void multipleTransactionsWithSameTimestampAreReportedCorrectly() throws Exception {
        api_cache.setReportExpirationInterval(50L);
        api_cache.report(createTransactionData());
        api_cache.report(createTransactionData());
        Thread.sleep(800L);
        verify(sender).sendPostToServer(SERVER_URL, RESPONSE_MULTIPLE_TRANSACTION);
    }

    protected static final String RESPONSE_DATA =
            "provider_key=" + PROVIDER_KEY + "&" +
                    "transactions[0][app_id]=bad7e480&" +
                    "transactions[0][usage][transfer]=2840&" +
                    "transactions[0][usage][hits]=1&" +
                    "transactions[0][timestamp]=2009-01-01+18%3A11%3A59&" +
                    "transactions[1][app_id]=bce4c8f4&" +
                    "transactions[1][usage][transfer]=4500&" +
                    "transactions[1][usage][hits]=1&" +
                    "transactions[1][timestamp]=2009-01-01+14%3A23%3A08";

    protected static final String MULTIPLE_RESPONSE_DATA =
            "provider_key=" + PROVIDER_KEY + "&" +
                    "transactions[0][app_id]=bad7e443&" +
                    "transactions[0][usage][transfer]=2800&" +
                    "transactions[0][usage][hits]=11&" +
                    "transactions[0][timestamp]=2009-01-01+18%3A11%3A58&" +
                    "transactions[1][app_id]=bad7e480&" +
                    "transactions[1][usage][transfer]=2840&" +
                    "transactions[1][usage][hits]=1&" +
                    "transactions[1][timestamp]=2009-01-01+18%3A11%3A59&" +
                    "transactions[2][app_id]=bce4c8f4&" +
                    "transactions[2][usage][transfer]=4500&" +
                    "transactions[2][usage][hits]=1&" +
                    "transactions[2][timestamp]=2009-01-01+14%3A23%3A08&" +
                    "transactions[3][app_id]=bce4c8f4&" +
                    "transactions[3][usage][transfer]=1500&" +
                    "transactions[3][usage][hits]=4&" +
                    "transactions[3][timestamp]=2009-01-01+14%3A23%3A20";

    protected static final String RESPONSE_MULTIPLE_TRANSACTION =
            "provider_key=" + PROVIDER_KEY + "&" +
                    "transactions[0][app_id]=bad7e480&" +
                    "transactions[0][usage][transfer]=2840&" +
                    "transactions[0][usage][hits]=1&" +
                    "transactions[0][timestamp]=2009-01-01+18%3A11%3A59&" +
                    "transactions[1][app_id]=bad7e480&" +
                    "transactions[1][usage][transfer]=2840&" +
                    "transactions[1][usage][hits]=1&" +
                    "transactions[1][timestamp]=2009-01-01+18%3A11%3A59&" +
                    "transactions[2][app_id]=bce4c8f4&" +
                    "transactions[2][usage][transfer]=4500&" +
                    "transactions[2][usage][hits]=1&" +
                    "transactions[2][timestamp]=2009-01-01+14%3A23%3A08&" +
                    "transactions[3][app_id]=bce4c8f4&" +
                    "transactions[3][usage][transfer]=4500&" +
                    "transactions[3][usage][hits]=1&" +
                    "transactions[3][timestamp]=2009-01-01+14%3A23%3A08";



    private ApiTransaction[] createTransactionData() {
        ApiTransaction[] transactions = new ApiTransaction[2];
        HashMap<String, String> metrics0 = new HashMap<String, String>();
        metrics0.put("hits", "1");
        metrics0.put("transfer", "4500");

        HashMap<String, String> metrics1 = new HashMap<String, String>();
        metrics1.put("hits", "1");
        metrics1.put("transfer", "2840");

        transactions[0] = new ApiTransactionForAppId("bce4c8f4", "2009-01-01 14:23:08", metrics0);
        transactions[1] = new ApiTransactionForAppId("bad7e480", "2009-01-01 18:11:59", metrics1);
        return transactions;
    }

    private ApiTransaction[] createSecondSetOfTransactionData() {
        ApiTransaction[] transactions = new ApiTransaction[2];
        HashMap<String, String> metrics0 = new HashMap<String, String>();
        metrics0.put("hits", "4");
        metrics0.put("transfer", "1500");

        HashMap<String, String> metrics1 = new HashMap<String, String>();
        metrics1.put("hits", "11");
        metrics1.put("transfer", "2800");

        transactions[0] = new ApiTransactionForAppId("bce4c8f4", "2009-01-01 14:23:20", metrics0);
        transactions[1] = new ApiTransactionForAppId("bad7e443", "2009-01-01 18:11:58", metrics1);
        return transactions;
    }

}
