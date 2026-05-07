export async function uploadRoomsFile({ name, file }) {
  const formData = new FormData();
  formData.append("name", name);
  formData.append("file", file);

  const response = await fetch("http://127.0.0.1:8000/optimization_problems/rooms/upload/", {
    method: "POST",
    body: formData,
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data?.error || data?.detail || "Erro ao fazer upload do ficheiro de salas.");
  }

  return data;
}

export async function getProblemRoomsMappingSuggestions(problemId) {
  const response = await fetch(
    `http://127.0.0.1:8000/optimization_problems/${problemId}/rooms-mapping-suggestions/`
  );

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data?.error || data?.detail || "Erro ao obter sugestões de mapping.");
  }

  return data;
}

export async function saveProblemRoomsMapping(problemId, payload) {
  const response = await fetch(
    `http://127.0.0.1:8000/optimization_problems/${problemId}/rooms-mapping-save/`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    }
  );

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data?.error || data?.detail || "Erro ao guardar mapping das salas.");
  }

  return data;
}