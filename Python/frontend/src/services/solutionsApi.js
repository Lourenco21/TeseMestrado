const API_BASE_URL = "http://localhost:8000/optimization_problems/";

async function handleResponse(response) {
  const contentType = response.headers.get("content-type") || "";

  let data = null;

  if (contentType.includes("application/json")) {
    data = await response.json().catch(() => null);
  } else {
    const text = await response.text().catch(() => "");
    throw new Error(
      `Resposta não JSON recebida do backend. Status ${response.status}. Conteúdo inicial: ${text.slice(0, 120)}`
    );
  }

  if (!response.ok) {
    const errorMessage =
      data?.detail ||
      data?.message ||
      data?.error ||
      "Ocorreu um erro no pedido à API de soluções.";
    throw new Error(errorMessage);
  }

  return data;
}

export async function listProblemSolutions(problemId) {
  const response = await fetch(`${API_BASE_URL}${problemId}/solutions/`);
  return handleResponse(response);
}

export async function getProblemSolutionDetail(problemId, solutionId) {
  const response = await fetch(
    `${API_BASE_URL}${problemId}/solutions/${solutionId}/`
  );
  return handleResponse(response);
}

export async function getProblemSolutionMetrics(problemId, solutionId) {
  const response = await fetch(
    `${API_BASE_URL}${problemId}/solutions/${solutionId}/metrics/`
  );

  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({}));
    throw new Error(errorBody.error || "Erro ao carregar métricas da solução.");
  }

  return response.json();
}