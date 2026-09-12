package io.github.alexistrejo11.pimienta.config.aws;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Wires S3 with static keys from Spring {@code Environment} ({@code .env} is merged by
 * {@link io.github.alexistrejo11.pimienta.config.env.DotenvEnvironmentPostProcessor}). The AWS SDK’s
 * default chain only reads OS env / {@code ~/.aws/credentials}, not Spring properties.
 *
 * <p>When {@code aws.s3.endpoint} is set (LocalStack in {@code dev}), clients use path-style access
 * and that override. Presigned URLs use {@code aws.s3.public-endpoint} when present so browsers on
 * the host can download objects.
 */
@Configuration
public class AwsConfig {

  @Value("${aws.region}")
  private String region;

  @Bean(destroyMethod = "close")
  public S3Client s3Client(
      @Value("${AWS_ACCESS_KEY_ID:}") String accessKeyId,
      @Value("${AWS_SECRET_ACCESS_KEY:}") String secretAccessKey,
      @Value("${aws.s3.endpoint:}") String endpoint) {
    var b = S3Client.builder().region(Region.of(region)).credentialsProvider(credentials(accessKeyId, secretAccessKey));
    if (StringUtils.hasText(endpoint)) {
      b.endpointOverride(URI.create(endpoint.trim())).forcePathStyle(true);
    }
    return b.build();
  }

  @Bean(destroyMethod = "close")
  public S3Presigner s3Presigner(
      @Value("${AWS_ACCESS_KEY_ID:}") String accessKeyId,
      @Value("${AWS_SECRET_ACCESS_KEY:}") String secretAccessKey,
      @Value("${aws.s3.endpoint:}") String endpoint,
      @Value("${aws.s3.public-endpoint:}") String publicEndpoint) {
    var b =
        S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(credentials(accessKeyId, secretAccessKey));
    String presignEndpoint = StringUtils.hasText(publicEndpoint) ? publicEndpoint.trim() : endpoint.trim();
    if (StringUtils.hasText(presignEndpoint)) {
      b.endpointOverride(URI.create(presignEndpoint))
          .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
    }
    return b.build();
  }

  private static AwsCredentialsProvider credentials(String accessKeyId, String secretAccessKey) {
    if (!accessKeyId.isBlank() && !secretAccessKey.isBlank()) {
      return StaticCredentialsProvider.create(
          AwsBasicCredentials.create(accessKeyId.trim(), secretAccessKey.trim()));
    }
    return DefaultCredentialsProvider.create();
  }
}
