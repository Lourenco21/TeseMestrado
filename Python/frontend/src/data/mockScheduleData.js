export const classOptions = [
  { id: "turma-a", label: "Turma A" },
  { id: "turma-b", label: "Turma B" },
];

export const subjectOptions = [
  { id: "pgmv", label: "Programação e Geração de Mundos Virtuais" },
  { id: "pa", label: "Programação Avançada" },
  { id: "gcco", label: "Gestão do Conhecimento e Cultura Organizacional" },
];

export const schedulesByClass = {
  "turma-a": [
    {
      id: 1,
      day: 2,
      start: "13:00",
      end: "14:30",
      title: "Programação e Geração de Mundos Virtuais",
      subtitle: "[03769] PGMVT01 - D1.07",
    },
    {
      id: 2,
      day: 2,
      start: "14:30",
      end: "16:00",
      title: "Programação e Geração de Mundos Virtuais",
      subtitle: "[03769] PGMVTP02 - D1.07",
    },
    {
      id: 3,
      day: 3,
      start: "13:00",
      end: "14:30",
      title: "Programação Avançada",
      subtitle: "[M4310] M4310T01 - C5.07",
    },
    {
      id: 4,
      day: 3,
      start: "14:30",
      end: "16:00",
      title: "Programação Avançada",
      subtitle: "[M4310] M4310TP02 - C5.07",
    },
    {
      id: 5,
      day: 5,
      start: "13:00",
      end: "14:30",
      title: "Gestão do Conhecimento e Cultura Organizacional",
      subtitle: "[M8683] M8683TP02",
    },
    {
      id: 6,
      day: 5,
      start: "14:30",
      end: "16:00",
      title: "Gestão do Conhecimento e Cultura Organizacional",
      subtitle: "[M8683] M8683TP02",
    },
    {
      id: 7,
      day: 1,
      start: "18:00",
      end: "20:00",
      title: "Ética Profissional, Computação e Sociedade",
      subtitle: "[03718] EPCSS03 - C5.07",
    },
    {
      id: 8,
      day: 2,
      start: "18:00",
      end: "19:30",
      title: "Blockchain",
      subtitle: "[03691] BkcnTP02 - C6.08",
    },
    {
      id: 9,
      day: 2,
      start: "19:30",
      end: "21:00",
      title: "Blockchain",
      subtitle: "[03691] BkcnTP02 - C6.08",
    },
  ],
  "turma-b": [
    {
      id: 10,
      day: 4,
      start: "10:00",
      end: "12:00",
      title: "Sistemas Distribuídos",
      subtitle: "[04000] SDT01 - B2.03",
    },
  ],
};

export const schedulesBySubject = {
  pgmv: [
    {
      id: 1,
      day: 2,
      start: "13:00",
      end: "14:30",
      title: "Programação e Geração de Mundos Virtuais",
      subtitle: "Turma A - D1.07",
    },
    {
      id: 2,
      day: 2,
      start: "14:30",
      end: "16:00",
      title: "Programação e Geração de Mundos Virtuais",
      subtitle: "Turma A - D1.07",
    },
  ],
  pa: [
    {
      id: 3,
      day: 3,
      start: "13:00",
      end: "14:30",
      title: "Programação Avançada",
      subtitle: "Turma A - C5.07",
    },
    {
      id: 4,
      day: 3,
      start: "14:30",
      end: "16:00",
      title: "Programação Avançada",
      subtitle: "Turma A - C5.07",
    },
  ],
  gcco: [
    {
      id: 5,
      day: 5,
      start: "13:00",
      end: "14:30",
      title: "Gestão do Conhecimento e Cultura Organizacional",
      subtitle: "Turma A",
    },
    {
      id: 6,
      day: 5,
      start: "14:30",
      end: "16:00",
      title: "Gestão do Conhecimento e Cultura Organizacional",
      subtitle: "Turma A",
    },
  ],
};