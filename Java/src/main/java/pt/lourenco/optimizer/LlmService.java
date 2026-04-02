package pt.lourenco.optimizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

@Service
public class LlmService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LlmService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public JsonNode askBestAlgorithm(String problemJson) throws Exception {
        String url = "http://localhost:11434/api/generate";

        // Montar prompt simples (ajusta depois)
        String prompt = """
                You are an expert in metaheuristics and in jMetal (Java framework for single-objective and multi-objective optimization).

                YOU MUST FOLLOW THESE RULES:
                - ANSWER ONLY WITH ONE VALID JSON OBJECT.
                - DO NOT WRITE ANY TEXT BEFORE OR AFTER THE JSON.
                - DO NOT USE MARKDOWN.
                - THE JSON MUST BE STRICTLY VALID (no comments, no trailing commas).
                
                YOUR TASK:
                You will receive ONE optimization problem described in JSON.
                The problem JSON is generic and domain-agnostic: it can describe scheduling, routing, assignment, design, etc.
                Based only on that problem description, you must:
                
                1) Analyze the problem type:
                   - single-objective or multi-objective
                   - number and type of decision variables (discrete, continuous, mixed)
                   - presence and type of constraints
                   - approximate scale (small / medium / large)
                
                2) Choose the SINGLE most appropriate optimization algorithm from jMetal.
                   - You may choose from any algorithm family available in jMetal: evolutionary algorithms, swarm intelligence, local search, multi-objective metaheuristics, etc.
                   - The choice MUST be justified in natural language referencing the problem characteristics.
                
                3) Configure the algorithm:
                   - Set all important parameters (population or swarm size, max evaluations or iterations, crossover/mutation operators or neighborhood operators, probabilities, distribution indexes, etc.).
                   - Ensure configuration is reasonable for the problem scale inferred from the JSON.
                
                4) Generate ALL the Java code needed to run the optimization using jMetal:
                   - A Problem class implementing the correct jMetal interface (single-objective or multi-objective).
                   - A solution representation / encoding class if a custom encoding is needed.
                   - An Algorithm runner class (a main class) that:
                     - instantiates the problem
                     - configures the algorithm with the chosen operators and parameters
                     - executes the algorithm
                     - prints or saves the final solutions
                   - Any helper classes that are strictly necessary for compilation and execution.
                
                IMPORTANT CODE CONSTRAINTS:
                - The code MUST be compatible with Java 17.
                - The code MUST be compatible with jMetal version 6.1. Use the official API of that version.
                - Do NOT use example problems from jMetal (like ZDT, DTLZ, Kursawe, Sphere, etc.).
                - Every class name you reference MUST be fully implemented in the output.
                - The evaluate() methods MUST NOT return a value; they must set each objective on the solution using solution.setObjective(index, value).
                - You MUST NOT output placeholder code such as "implement here", "TODO", or references to undefined helpers. All logic must be implemented end-to-end.
                
                INPUT FORMAT:
                You receive the problem JSON between <<<PROBLEM_JSON>>> and <<<END_PROBLEM_JSON>>>.
                Do NOT repeat the problem JSON in the output.
                
                OUTPUT FORMAT:
                You must return ONLY ONE JSON object with this structure:
                
                {
                  "algorithm_name": "string",
                  "algorithm_family": "string", 
                  "algorithm_reason": "string",
                  "algorithm_configuration": {
                    "parameters": {
                      "population_size": 0,
                      "max_evaluations": 0,
                      "crossover_operator": "string or null",
                      "crossover_probability": 0.0,
                      "crossover_distribution_index": 0.0,
                      "mutation_operator": "string or null",
                      "mutation_probability": 0.0,
                      "mutation_distribution_index": 0.0,
                      "other_parameters": {
                        "key": "value"
                      }
                    }
                  },
                  "java_code": {
                    "problem_class": "FULL JAVA CODE OF THE PROBLEM CLASS HERE",
                    "solution_class": "FULL JAVA CODE OF THE SOLUTION/ENCODING CLASS HERE (if needed)",
                    "algorithm_runner_class": "FULL JAVA CODE OF A MAIN/TEST CLASS THAT CONFIGURES AND RUNS THE ALGORITHM",
                    "additional_helpers": [
                      "FULL JAVA CODE OF ANY ADDITIONAL CLASS NEEDED FOR COMPILATION",
                      "OPTIONAL SECOND CLASS"
                    ]
                  }
                }
                
                NOW READ THE PROBLEM JSON AND ANSWER ONLY WITH THE JSON OBJECT ABOVE (NO EXTRA TEXT).
                
                <<<PROBLEM_JSON>>>""" +

                createJson()+"""
                <<<END_PROBLEM_JSON>>>
                """;


        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama3");       // ou a tag concreta que estás a usar
        body.put("prompt", prompt);
        body.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity =
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(url, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Erro ao chamar LLM: " + response.getStatusCode());
        }


        JsonNode root = objectMapper.readTree(response.getBody());

        return root.get("response");
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static JsonNode createJson() {
        String json = """
        {
          "decision_variables": [
            {
              "name": "Sala",
              "length": 2000,
              "domain": [0, 130]
            }
          ],
          "objectives": [
            {
              "id": "f1",
              "label": "Capacidade de Sala superior aos inscritos na Aula",
              "goal": "minimize",
              "expression": "?[aula.inscritos > sala.capacidade].size()"
            },
            {
              "id": "f2",
              "label": "Não pode haver 2 aulas na mesma sala ao mesmo tempo",
              "goal": "minimize",
              "expression": "#helper.countAulasWithConflicts()"
            },
            {
              "id": "f3",
              "label": "Não pode haver 2 aulas na mesma sala ao mesmo tempo",
              "goal": "minimize",
              "expression": "#helper.countAulasWithConflicts()"
            }
          ],
          "constraints": [
            {
              "id": "c1",
              "label": "Não se podem desperdiçar mais de 20 lugares",
              "expression": "#helper.countWaste(20)"
            }
          ]
        }
        """;

        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Erro a criar JsonNode", e);
        }
    }
}

