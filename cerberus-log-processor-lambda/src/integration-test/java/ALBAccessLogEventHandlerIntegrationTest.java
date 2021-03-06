import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.waf.AWSWAFRegionalClient;
import com.fieldju.commons.EnvUtils;
import com.fieldju.commons.PropUtils;
import com.google.common.collect.Lists;
import com.nike.cerberus.lambda.waf.LogProcessorLambdaConfig;
import com.nike.cerberus.lambda.waf.handler.ALBAccessLogEventHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * An integration test that can be manipulated for UAT testing on the fly without needing to curl requests through the gateway
 */
public class ALBAccessLogEventHandlerIntegrationTest {

    private ALBAccessLogEventHandler handler;

    @Mock
    S3Event event;

    @Before
    public void before() throws IOException {
        initMocks(this);

        String environment = PropUtils.getRequiredProperty("CERBERUS_ENV");
        String bucketName = PropUtils.getRequiredProperty("S3_BUCKET_NAME");
        String region = PropUtils.getRequiredProperty("REGION");
        String logKey = PropUtils.getRequiredProperty("S3_LOG_FILE_KEY");
        String manualBlacklistIpSetId = PropUtils.getRequiredProperty("BLACKLIST_IP_SET_ID");
        String manualWhitelistIpSetId = PropUtils.getRequiredProperty("WHITELIST_IP_SET_ID");
        String rateLimitAutoBlacklistIpSetId = PropUtils.getRequiredProperty("RATE_LIMIT_IP_SET_ID");
        String athenaDatabaseName = EnvUtils.getRequiredEnv("ATHENA_DATABASE_NAME");
        String athenaTableName = EnvUtils.getRequiredEnv("ATHENA_TABLE_NAME");
        String albLogBucketName = EnvUtils.getRequiredEnv("ALB_LOG_BUCKET");
        String athenaQueryResultBucketName = EnvUtils.getRequiredEnv("ATHENA_QUERY_RESULT_BUCKET_NAME");
        String iamPrincipalArn = EnvUtils.getRequiredEnv("IAM_PRINCIPAL_ARN");

        handler = new ALBAccessLogEventHandler(
                new AmazonS3Client(),
                AWSWAFRegionalClient.builder().withRegion(region).build(),
                new LogProcessorLambdaConfig(
                        environment,
                        manualWhitelistIpSetId,
                        manualBlacklistIpSetId,
                        rateLimitAutoBlacklistIpSetId,
                        60,
                        100,
                        null,
                        null,
                        athenaDatabaseName,
                        athenaTableName,
                        athenaQueryResultBucketName,
                        albLogBucketName,
                        iamPrincipalArn,
                        Regions.fromName(region)));

        List<S3EventNotification.S3EventNotificationRecord> records = Lists.newArrayList();
        S3EventNotification.S3EventNotificationRecord record = mock(S3EventNotification.S3EventNotificationRecord.class);
        records.add(record);

        when(event.getRecords()).thenReturn(records);

        S3EventNotification.S3Entity s3Entity = mock(S3EventNotification.S3Entity.class);
        S3EventNotification.S3BucketEntity bucketEntity = mock(S3EventNotification.S3BucketEntity.class);
        S3EventNotification.S3ObjectEntity objectEntity = mock(S3EventNotification.S3ObjectEntity.class);
        when(s3Entity.getBucket()).thenReturn(bucketEntity);
        when(s3Entity.getObject()).thenReturn(objectEntity);

        when(record.getS3()).thenReturn(s3Entity);

        when(bucketEntity.getName()).thenReturn(bucketName);
        when(objectEntity.getKey()).thenReturn(logKey);
    }

    @Test
    public void endToEndTest() throws IOException {
        // this test doesn't actually assert anything, but it allows me to trigger the lambda in a
        // controlled way and verify things manually, and attach a debugger to real running code
        handler.handleScheduledEvent();
    }

}
