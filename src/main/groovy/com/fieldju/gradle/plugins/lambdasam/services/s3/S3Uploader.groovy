package com.fieldju.gradle.plugins.lambdasam.services.s3

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.Upload
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.security.MessageDigest

/**
 * A service loosely based off of the AWS CLI CloudFormation Package command s3uploader
 * see: https://github.com/aws/aws-cli/blob/1.11.56/awscli/customizations/cloudformation/s3uploader.py
 */
class S3Uploader {

    Logger logger = Logging.getLogger(getClass())

    private final String kmsKeyId
    private final boolean forceUploads
    private final TransferManager transferManager

    S3Uploader(String region, String kmsKeyId, boolean forceUploads) {
        transferManager = new TransferManager(
                AmazonS3Client.builder()
                        .standard()
                        .withRegion(Regions.fromName(region)).build())

        this.kmsKeyId = kmsKeyId
        this.forceUploads = forceUploads
    }

    /**
     * Makes and returns name of the S3 object based on the file's MD5 sum
     *
     * @param fileName file to upload
     * @param extension  String of file extension to append to the object
     * @return S3 URL of the uploaded object
     */
    String uploadWithDedup(String bucket, String prefix, final File file) {
        def md5Hash = MessageDigest.getInstance('MD5').digest(file.getBytes()).encodeHex().toString()
        def ext = file.getName().find(/^.*?(\..*$)/){m,m2 -> return m2}
        def key = ''
        key += prefix ? "$prefix/" : ''
        key += md5Hash
        key += ext ? ext : ''

        def s3Uri = "s3://${bucket}/${key}"

        logger.lifecycle("Generated md5 hash: ${md5Hash} attempting to put ${file.getName()} at ${s3Uri}}")
        if (! forceUploads && transferManager.getAmazonS3Client().doesObjectExist(bucket, key)) {
            logger.lifecycle("File: ${file.absolutePath} with MD5: ${md5Hash} already uploaded at: ${s3Uri} skipping ...")
            return s3Uri
        }

        logger.lifecycle("Uploading ${file.absolutePath} to ${s3Uri}")
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, file)

        // either use kms or server side encryption
        if (kmsKeyId != null && kmsKeyId.trim() != "") {
            putObjectRequest.withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams(kmsKeyId))
        } else {
            ObjectMetadata objectMetadata = new ObjectMetadata()
            objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION)
            putObjectRequest.withMetadata(objectMetadata)
        }
        Upload upload = transferManager.upload(putObjectRequest)

        while (! upload.isDone()) {
            logger.lifecycle("${upload.getProgress().getPercentTransferred().round(2)}% | Uploading to ${s3Uri}")
            sleep(500)
        }
        logger.lifecycle("Upload complete. S3 Uri: ${s3Uri}")

        upload.waitForUploadResult()
        return s3Uri
    }
}
