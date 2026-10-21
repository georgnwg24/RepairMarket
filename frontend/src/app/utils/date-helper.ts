import { formatDate } from "@angular/common";

export function formatIsoDate(date: Date): string {
  return formatDate(date, "yyyy-MM-dd", "en-DK");
}

export function formatDateToHourMinute(date: Date): string {
  return formatDate(date, "HH:mm", "en-DK");
}

export function chatMessageFormatted(input: string | Date): string {
  const date = (input instanceof Date) ? input : new Date(input);
  const now = new Date()
  const yesterday = new Date(now);

  yesterday.setDate(now.getDate() - 1); // not in one-line possible (converts to number??)

  const isToday = date.toDateString() === now.toDateString();
  const isYesterday = date.toDateString() === yesterday.toDateString();
  const isThisYear = date.getFullYear() === now.getFullYear();

  const pad = (n: number) => n.toString().padStart(2, "0");

  const hours = pad(date.getHours());
  const minutes = pad(date.getMinutes());

  if (isToday) {
    return `${hours}:${minutes}`; // 15:19
  }

  if (isYesterday) {
    return `yesterday ${hours}:${minutes}`;
  }

  const day = pad(date.getDate());
  const month = pad(date.getMonth() + 1);

  if (isThisYear) {
    return `${day}.${month} ${hours}:${minutes}`; // 07.01 12:35
  }

  return `${day}.${month}.${date.getFullYear()} ${hours}:${minutes}`; // 12.06.2024 14:12
}

export function chatPreviewFormatted(input: string | Date | undefined): string {
  if (!input) {
    return "";
  }
  const date = typeof input === 'string' ? new Date(input) : input;
  const now = new Date();
  const deltaMs = now.getTime() - date.getTime();
  const deltaMins = Math.floor(deltaMs / 60000);
  const deltaHours = Math.floor(deltaMs / 3600000);
  const deltaDays = Math.floor(deltaMs / 86400000);

  if (deltaMins < 1) {
    return "just now";
  }
  if (deltaMins < 60) {
    return `${deltaMins} min${deltaMins === 1 ? '' : 's'} ago`;
  }
  if (deltaHours < 24) {
    return `${deltaHours} hour${deltaHours === 1 ? '' : 's'} ago`;
  }

  if (deltaDays === 1) {
    return "yesterday";
  }
  if (deltaDays < 31) {
    return `${deltaDays} day${deltaDays === 1 ? '' : 's'} ago`;
  }

  // older than a month
  const opts: Intl.DateTimeFormatOptions = {
    localeMatcher: "best fit",
    day: '2-digit',
    month: '2-digit',
    ...(date.getFullYear() !== now.getFullYear() ? { year: 'numeric' } : {})
  };
  return date.toLocaleDateString("de_AT", opts);
}
