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

export async function sendProblemToJava(problemId, executionConfig = {}) {
  const response = await fetch(`http://127.0.0.1:8000/optimization_problems/${problemId}/send-to-java/`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(executionConfig),
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.error || "Não foi possível enviar o problema para Java.");
  }

  return data;
}

export async function requestProblemAlgorithms(problemId, executionConfig = {}) {
  const response = await fetch(
    `http://127.0.0.1:8000/optimization_problems/${problemId}/request-algorithms/`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(executionConfig),
    }
  );

  const contentType = response.headers.get("content-type") || "";

  let data;
  if (contentType.includes("application/json")) {
    data = await response.json();
  } else {
    const text = await response.text();
    throw new Error(
      `Resposta não JSON recebida do backend. Status ${response.status}. Conteúdo inicial: ${text.slice(0, 120)}`
    );
  }

  if (!response.ok) {
    const message = [data.error, data.details]
      .filter(Boolean)
      .join(" | ");

    throw new Error(message || "Não foi possível pedir algoritmos.");
  }

  return data;
}




