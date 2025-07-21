package com.utk.rds.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;

@Component
@Profile("create-rds")
public class RdsCreator implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(RdsCreator.class);

    private final RdsClient rdsClient;
    private final SecretsManagerClient secretsManagerClient;

    @Value("${rds.dbName}")
    private String dbName;

    @Value("${rds.instanceIdentifier}")
    private String dbInstanceIdentifier;

    @Value("${rds.username}")
    private String masterUsername;

    @Value("${rds.password}")
    private String masterPassword;

    @Value("${rds.secretName}")
    private String secretName;

    public RdsCreator(RdsClient rdsClient, SecretsManagerClient secretsManagerClient) {
        this.rdsClient = rdsClient;
        this.secretsManagerClient = secretsManagerClient;
    }

    @Override
    public void run(String... args) throws Exception {
        createRdsInstance();
    }

    private void createRdsInstance() throws InterruptedException {
        // Save password to Secrets Manager
        try {
            secretsManagerClient.createSecret(CreateSecretRequest.builder()
                    .name(secretName)
                    .secretString(masterPassword)
                    .build());
        } catch (Exception e) {
            logger.info("Secret already exists, skipping creation.");
        }

        // Create RDS Instance
        CreateDbInstanceRequest request = CreateDbInstanceRequest.builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .allocatedStorage(20)
                .dbInstanceClass("db.t3.micro")
                .engine("mysql")
                .masterUsername(masterUsername)
                .masterUserPassword(masterPassword)
                .dbName(dbName)
                .vpcSecurityGroupIds()
                .publiclyAccessible(true)
                .backupRetentionPeriod(1)
                .multiAZ(false)
                .build();

        rdsClient.createDBInstance(request);
        logger.info("Waiting for DB to become available...");

        boolean available = false;
        while (!available) {
            Thread.sleep(30000);
            DescribeDbInstancesResponse describe = rdsClient.describeDBInstances(
                    DescribeDbInstancesRequest.builder()
                            .dbInstanceIdentifier(dbInstanceIdentifier)
                            .build()
            );

            String status = describe.dbInstances().get(0).dbInstanceStatus();
            logger.info("Current status: {}", status);
            if ("available".equalsIgnoreCase(status)) {
                available = true;
                logger.info("✅ DB Instance available at: {}:{}", 
                    describe.dbInstances().get(0).endpoint().address(),
                    describe.dbInstances().get(0).endpoint().port()
                );
            }
        }
    }
}
