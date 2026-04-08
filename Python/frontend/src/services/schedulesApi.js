export async function fetchClasses() {
  const response = await fetch("/api/classes/");
  if (!response.ok) throw new Error("Erro ao obter turmas");
  return response.json();
}

export async function fetchScheduleByClass(classId) {
  const response = await fetch(`/api/schedules/classes/${classId}/`);
  if (!response.ok) throw new Error("Erro ao obter horário da turma");
  return response.json();
}

export async function uploadScheduleFile({ name, file }) {
  const formData = new FormData();
  formData.append("name", name);
  formData.append("file", file);

  const response = await fetch("http://127.0.0.1:8000/schedules/upload/", {
    method: "POST",
    body: formData,
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data?.file?.[0] || data?.name?.[0] || "Erro no upload");
  }

  return data;
}

export async function getScheduleMappingSuggestions(scheduleId) {
  const response = await fetch(
    `http://127.0.0.1:8000/schedules/${scheduleId}/mapping-suggestions/`
  );

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data?.error || "Erro ao obter sugestões de mapping");
  }

  return data;
}

export async function saveScheduleMapping(scheduleId, mappings) {
  const response = await fetch(
    `http://127.0.0.1:8000/schedules/${scheduleId}/save-mapping/`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ mappings }),
    }
  );

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data?.error || "Erro ao guardar mapping");
  }

  return data;
}

export async function getSchedules() {
  const response = await fetch("http://127.0.0.1:8000/schedules/");
  const data = await response.json();

  if (!response.ok) {
    throw new Error("Erro ao obter horários");
  }

  return data;
}