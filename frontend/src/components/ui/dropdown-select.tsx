import {
  Check,
  ChevronDown,
} from "lucide-react";
import {
  type CSSProperties,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from "react";

export type DropdownSelectOption = {
  value: string;
  label: string;
  description?: string;
};

export function DropdownSelect({
  label,
  value,
  placeholder,
  options,
  onChange,
  className,
  listMaxHeight = 176,
  constrainToModal = false,
}: {
  label?: string;
  value: string;
  placeholder: string;
  options: DropdownSelectOption[];
  onChange: (value: string) => void;
  className?: string;
  listMaxHeight?: number;
  constrainToModal?: boolean;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [
    menuLayout,
    setMenuLayout,
  ] = useState<{
    placement: "bottom" | "top";
    maxHeight: number;
  }>({
    placement: "bottom",
    maxHeight: listMaxHeight,
  });
  const wrapperRef = useRef<HTMLDivElement | null>(null);

  const selectedOption =
    options.find((option) => option.value === value) ?? null;

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      if (
        wrapperRef.current &&
        !wrapperRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setIsOpen(false);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, []);

  useLayoutEffect(() => {
    if (!isOpen) {
      return;
    }

    function updateMenuLayout() {
      const wrapper = wrapperRef.current;

      if (!wrapper) {
        return;
      }

      const modalPanel = constrainToModal
        ? wrapper.closest(".site-modal-panel")
        : null;

      if (!(modalPanel instanceof HTMLElement)) {
        setMenuLayout({
          placement: "bottom",
          maxHeight: listMaxHeight,
        });
        return;
      }

      const gap = 6;
      const boundaryPadding = 12;
      const wrapperRect = wrapper.getBoundingClientRect();
      const modalRect = modalPanel.getBoundingClientRect();
      const availableBelow =
        modalRect.bottom - wrapperRect.bottom - gap - boundaryPadding;
      const availableAbove =
        wrapperRect.top - modalRect.top - gap - boundaryPadding;
      const placement =
        availableBelow >= Math.min(listMaxHeight, 120) ||
        availableBelow >= availableAbove
          ? "bottom"
          : "top";
      const availableSpace =
        placement === "bottom"
          ? availableBelow
          : availableAbove;

      setMenuLayout({
        placement,
        maxHeight: Math.max(
          48,
          Math.min(
            listMaxHeight,
            Math.floor(availableSpace)
          )
        ),
      });
    }

    updateMenuLayout();

    window.addEventListener("resize", updateMenuLayout);
    window.addEventListener("scroll", updateMenuLayout, true);

    return () => {
      window.removeEventListener("resize", updateMenuLayout);
      window.removeEventListener("scroll", updateMenuLayout, true);
    };
  }, [
    constrainToModal,
    isOpen,
    listMaxHeight,
  ]);

  const menuPositionClassName =
    menuLayout.placement === "top"
      ? "bottom-[calc(100%+6px)]"
      : "top-[calc(100%+6px)]";

  const listStyle: CSSProperties = {
    maxHeight: menuLayout.maxHeight,
  };

  return (
    <div ref={wrapperRef} className="relative grid gap-2">
      {label ? (
        <span className="text-[11px] font-medium uppercase tracking-normal text-[var(--text-subtle)]">
          {label}
        </span>
      ) : null}

      <button
        type="button"
        onClick={() => setIsOpen((current) => !current)}
        className={[
          "v7-motion-fast v7-pressable flex w-full items-center justify-between gap-3 rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 text-left text-sm text-[var(--text-base)] outline-none hover:border-[var(--border-hover)] focus:border-[var(--border-hover)]",
          className ?? "h-[var(--control-height)]",
        ].join(" ")}
      >
        <span className={selectedOption ? "truncate text-[var(--text-base)]" : "truncate text-[var(--text-subtle)]"}>
          {selectedOption?.label ?? placeholder}
        </span>

        <ChevronDown
          className={[
            "h-4 w-4 shrink-0 text-[var(--text-muted)] transition duration-[var(--motion-fast)] ease-[var(--motion-ease)]",
            isOpen ? "rotate-180 text-[var(--color-accent)]" : "",
          ].join(" ")}
          aria-hidden="true"
        />
      </button>

      {isOpen ? (
        <div className={["dropdown-panel absolute left-0 right-0 z-[380] overflow-hidden rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-card-strong)] shadow-[var(--shadow-dropdown)]", menuPositionClassName].join(" ")}>
          <div
            className="premium-scroll overflow-y-auto py-1"
            style={listStyle}
          >
            {options.map((option) => {
              const selected = option.value === value;

              return (
                <button
                  key={option.value}
                  type="button"
                  onClick={() => {
                    onChange(option.value);
                    setIsOpen(false);
                  }}
                  className={[
                    "v7-motion-fast flex w-full items-center justify-between gap-3 px-3 py-2 text-left text-sm",
                    selected
                      ? "bg-[var(--surface-hover)] text-[var(--text-base)]"
                      : "text-[var(--text-muted)] hover:bg-[var(--surface-raised)] hover:text-[var(--text-base)]",
                  ].join(" ")}
                >
                  <span className="min-w-0">
                    <span className="block truncate font-medium">
                      {option.label}
                    </span>

                    {option.description ? (
                      <span className="mt-0.5 block truncate text-xs text-[var(--text-subtle)]">
                        {option.description}
                      </span>
                    ) : null}
                  </span>

                  {selected ? (
                    <Check
                      className="h-4 w-4 shrink-0 text-[var(--color-accent)]"
                      aria-hidden="true"
                    />
                  ) : null}
                </button>
              );
            })}

            {options.length === 0 ? (
              <p className="px-3 py-3 text-sm text-[var(--text-muted)]">
                Nenhuma opção disponível.
              </p>
            ) : null}
          </div>
        </div>
      ) : null}
    </div>
  );
}
