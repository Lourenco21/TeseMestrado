export function timeToMinutes(time) {
  const [hours, minutes] = time.split(":").map(Number);
  return hours * 60 + minutes;
}

export function getEventPosition(start, end, startHour = 9, slotHeight = 60) {
  const startMinutes = timeToMinutes(start);
  const endMinutes = timeToMinutes(end);
  const calendarStart = startHour * 60;

  const top = ((startMinutes - calendarStart) / 60) * slotHeight;
  const height = ((endMinutes - startMinutes) / 60) * slotHeight;

  return { top, height };
}

export function generateTimeSlots(startHour = 9, endHour = 21) {
  const slots = [];
  for (let hour = startHour; hour <= endHour; hour += 1) {
    slots.push(`${String(hour).padStart(2, "0")}:00`);
  }
  return slots;
}