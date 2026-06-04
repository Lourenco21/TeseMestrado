package pt.lourenco.optimization.django;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import pt.lourenco.optimization.django.config.DjangoApiProperties;
import pt.lourenco.optimization.django.dto.DjangoProblemDataResponse;

@Service
public class DjangoProblemDataClient {

    private final RestTemplate restTemplate;
    private final DjangoApiProperties properties;

    public DjangoProblemDataClient(
            RestTemplate restTemplate,
            DjangoApiProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public DjangoProblemDataResponse fetchProblemData(Integer problemId) {
        if (problemId == null) {
            throw new DjangoIntegrationException("Problem id is required.");
        }

        String url = buildUrl(problemId);

        try {
            ResponseEntity<DjangoProblemDataResponse> response =
                    restTemplate.getForEntity(url, DjangoProblemDataResponse.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new DjangoIntegrationException(
                        "Django returned non-success status: " + response.getStatusCode()
                );
            }

            DjangoProblemDataResponse body = response.getBody();
            if (body == null) {
                throw new DjangoIntegrationException("Django returned an empty response body.");
            }

            return body;
        } catch (RestClientException ex) {
            throw new DjangoIntegrationException("Failed to fetch problem data from Django.", ex);
        }
    }

    private String buildUrl(Integer problemId) {
        String baseUrl = properties.getBaseUrl();
        String path = properties.getProblemDataPath();

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new DjangoIntegrationException("Django base URL is not configured.");
        }

        if (path == null || path.isBlank()) {
            throw new DjangoIntegrationException("Django problem data path is not configured.");
        }

        return baseUrl + path.replace("{problemId}", String.valueOf(problemId));
    }
}