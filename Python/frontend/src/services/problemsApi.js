const API_BASE_URL = "http://localhost:8000/optimization_problems/";

async function handleResponse(response) {
  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const errorMessage =
      data?.detail ||
      data?.message ||
      "Ocorreu um erro no pedido à API de problemas.";
    throw new Error(errorMessage);
  }

  return data;
}

export async function listProblemDrafts() {
  const response = await fetch(API_BASE_URL);
  return handleResponse(response);
}

export async function getProblemDraft(problemId) {
  const response = await fetch(`${API_BASE_URL}${problemId}/`);
  return handleResponse(response);
}

export async function createProblemDraft(payload = {}) {
  const response = await fetch(API_BASE_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  return handleResponse(response);
}

export async function updateProblemDraft(problemId, payload) {
  const response = await fetch(`${API_BASE_URL}${problemId}/`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  return handleResponse(response);
}

export async function deleteProblemDraft(problemId) {
  const response = await fetch(`${API_BASE_URL}${problemId}/`, {
    method: "DELETE",
  });

  if (!response.ok) {
    const data = await response.json().catch(() => null);
    const errorMessage =
      data?.detail ||
      data?.message ||
      "Não foi possível apagar o problema.";
    throw new Error(errorMessage);
  }

  return true;
}

export async function getProblemMappingSuggestions(problemId) {
  const response = await fetch(
    `http://127.0.0.1:8000/optimization_problems/${problemId}/mapping-suggestions/`
  );
  const data = await response.json();

  if (!response.ok) {
    throw new Error(data?.error || "Erro ao obter sugestões de mapping.");
  }

  return data;
}

export async function getProblemCatalog() {
  const response = await fetch("http://127.0.0.1:8000/optimization_problems/catalog/");
  return handleResponse(response);
}