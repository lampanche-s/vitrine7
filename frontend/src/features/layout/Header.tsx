import {
  useEffect,
  useMemo,
  useState,
} from "react";
import {
  CalendarDays,
  LogOut,
} from "lucide-react";

function formatHeaderDate(date: Date) {
  const weekday = new Intl.DateTimeFormat("pt-BR", {
    weekday: "short",
  })
    .format(date)
    .replace(".", "")
    .toUpperCase();

  const day = String(date.getDate()).padStart(2, "0");
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const year = date.getFullYear();

  return `${weekday}, ${day}/${month}/${year}`;
}

function formatHeaderTime(date: Date) {
  return new Intl.DateTimeFormat("pt-BR", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(date);
}

export function Header({
  onLogout,
}: {
  onLogout: () => void;
}) {
  const [now, setNow] = useState(() => new Date());
  const [logoLoaded, setLogoLoaded] = useState(false);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      setNow(new Date());
    }, 1000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, []);

  const headerDate = useMemo(
    () => formatHeaderDate(now),
    [now]
  );

  const headerTime = useMemo(
    () => formatHeaderTime(now),
    [now]
  );

  return (
    <header className="header-glass relative z-30 flex h-[82px] shrink-0 items-center border-b border-[var(--border-subtle)] px-4 sm:px-6 lg:px-9">
      <div className="flex w-full items-center justify-between gap-4">
        <div className="relative aspect-[4076/577] w-[190px] max-w-[42vw] lg:w-[235px]">
            <span
              aria-hidden="true"
              className={[
                "absolute inset-0 flex items-center text-sm font-semibold tracking-normal text-[var(--text-muted)]",
                logoLoaded
                  ? "opacity-0"
                  : "opacity-100",
              ].join(" ")}
            >
              Vitrine 7
            </span>

            <img
              src="/vitrine-logo-layout.png"
              alt="Vitrine 7"
              width={4076}
              height={577}
              loading="eager"
              decoding="async"
              onLoad={() =>
                setLogoLoaded(true)
              }
              className="relative z-10 h-full w-full object-contain object-left"
            />
        </div>

        <div className="flex items-center gap-4 lg:gap-8">
          <div className="hidden items-center gap-3 sm:flex">
            <CalendarDays
              className="h-5 w-5 shrink-0 text-[var(--text-base)]"
              aria-hidden="true"
            />

            <div className="leading-none">
              <p className="text-[10px] font-medium uppercase text-[var(--text-base)]">
                {headerDate}
              </p>

              <p className="mt-1 text-[14px] font-medium text-[var(--text-base)]">
                {headerTime}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onLogout}
            className="v7-motion-fast v7-pressable flex items-center gap-2 rounded-[4px] px-2 py-2 text-sm font-medium text-[var(--text-base)] hover:bg-[var(--surface-raised)] hover:text-[var(--text-base)]"
          >
            <LogOut
              className="h-5 w-5"
              aria-hidden="true"
            />
            <span className="hidden sm:inline">
              Sair
            </span>
          </button>
        </div>
      </div>
    </header>
  );
}
