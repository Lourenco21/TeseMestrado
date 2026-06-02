package pt.lourenco.optimization.django;

import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(value = "djangoProblemData", key = "T(String).valueOf(#problemDraftId)")
    public DjangoProblemDataResponse fetchProblemData(Integer problemDraftId) {
        if (problemDraftId == null) {
            throw new DjangoIntegrationException("Problem draft id is required.");
        }

        String url = buildUrl(problemDraftId);

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

    private String buildUrl(Integer problemDraftId) {
        String baseUrl = properties.getBaseUrl();
        String path = properties.getProblemDataPath();

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new DjangoIntegrationException("Django base URL is not configured.");
        }

        if (path == null || path.isBlank()) {
            throw new DjangoIntegrationException("Django problem data path is not configured.");
        }

        return baseUrl + path.replace("{draftId}", String.valueOf(problemDraftId));
    }
}