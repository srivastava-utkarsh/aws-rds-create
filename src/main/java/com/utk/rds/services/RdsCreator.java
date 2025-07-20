package com.utk.rds.services;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;

@Service
public class RdsCreator {

	private final RdsClient rdsClient;

	public RdsCreator(RdsClient rdsClient) {
		this.rdsClient = RdsClient.builder()
								.region(Region.AP_SOUTH_1)
								.credentialsProvider(DefaultCredentialsProvider.create())
								.build();
	}

	@PostConstruct
	public void createDbInstance(){
		String dbInstanceId = "basic-crud-app";

		try {
			DescribeDbInstancesResponse describeResponse = rdsClient.describeDBInstances(DescribeDbInstancesRequest.builder().build());
			boolean exists = describeResponse.dbInstances().stream().anyMatch(db -> db.dbInstanceIdentifier().equalsIgnoreCase(dbInstanceId));
			if (exists) {
				System.out.println("DB instance already exists");
				return;
			}

			CreateDbInstanceRequest request = CreateDbInstanceRequest.builder()
																	.dbInstanceIdentifier(dbInstanceId)
																	.allocatedStorage(20)
																	.dbName("basic-crud-app")
																	.engine("mysql")
																	.masterUsername("admin")
																	.masterUserPassword("password")
																	.dbInstanceClass("db.t3.micro")
																	.availabilityZone("ap-south-1a")
																	.backupRetentionPeriod(0)
																	.publiclyAccessible(true)
																	.multiAZ(false)
																	.storageType("gp2")
																	.build();

			rdsClient.createDBInstance(request);
			System.out.println("RDS instance creation initiated.");
		} catch (DbInstanceAlreadyExistsException e) {
			System.out.println("DB instance already exists");
		} catch (RdsException e){
			e.printStackTrace();
		} catch (Exception e){
			System.out.println("Inside general exception block");
			e.printStackTrace();
		}
	}
}
