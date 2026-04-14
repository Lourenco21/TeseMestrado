PROBLEM_FAMILIES = [
    {
        "id": "scheduling",
        "label": "Scheduling",
        "description": "Distribuição de tarefas, eventos ou recursos ao longo do tempo.",
        "subtypes": [
            {"id": "workforce", "label": "Workforce Scheduling"},
            {"id": "project", "label": "Project Scheduling"},
            {"id": "appointment", "label": "Appointment Scheduling"},
        ],
    },
    {
        "id": "timetabling",
        "label": "Timetabling",
        "description": "Criação de horários para aulas, exames ou formação.",
        "subtypes": [
            {"id": "university_course_timetabling", "label": "University"},
            {"id": "school", "label": "School"},
            {"id": "exams", "label": "Exams"},
        ],
    },
    {
        "id": "allocation",
        "label": "Allocation",
        "description": "Atribuição de pessoas, recursos ou itens a entidades/slots.",
        "subtypes": [
            {"id": "staff", "label": "Staff Allocation"},
            {"id": "room", "label": "Room Allocation"},
            {"id": "asset", "label": "Asset Allocation"},
        ],
    },
]

OBJECTIVE_LIBRARY = {
    "timetabling": [
        {"id": "min_conflicts", "label": "Minimizar conflitos", "type": "hard_or_soft"},
        #{"id": "balance_load", "label": "Equilibrar carga", "type": "soft"},
        {"id": "maximize_preferences", "label": "Maximizar preferências", "type": "soft"},
    ],
    "scheduling": [
        {"id": "min_overtime", "label": "Minimizar horas extra", "type": "soft"},
        {"id": "respect_deadlines", "label": "Respeitar deadlines", "type": "hard_or_soft"},
    ],
    "allocation": [
        {"id": "min_unassigned", "label": "Minimizar não atribuídos", "type": "hard_or_soft"},
        {"id": "balance_utilization", "label": "Equilibrar utilização", "type": "soft"},
    ],
}

CONSTRAINT_LIBRARY = {
    "timetabling": [
        {
            "id": "no_teacher_overlap",
            "label": "Professor não pode estar em dois lugares ao mesmo tempo",
            "hard": True,
        },
        {
            "id": "no_room_overlap",
            "label": "Sala não pode ter duas aulas ao mesmo tempo",
            "hard": True,
        },
        {
            "id": "room_capacity",
            "label": "Capacidade da sala",
            "hard": True,
        },
        {
            "id": "teacher_availability",
            "label": "Disponibilidade do professor",
            "hard": True,
        },
    ],
    "scheduling": [
        {
            "id": "resource_availability",
            "label": "Disponibilidade do recurso",
            "hard": True,
        },
        {
            "id": "precedence",
            "label": "Respeitar precedências",
            "hard": True,
        },
    ],
    "allocation": [
        {
            "id": "unique_assignment",
            "label": "Atribuição única",
            "hard": True,
        },
        {
            "id": "capacity_limit",
            "label": "Limite de capacidade",
            "hard": True,
        },
    ],
}