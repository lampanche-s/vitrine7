import type {
  ButtonHTMLAttributes,
  ReactNode,
} from "react";

export type ButtonVariant =
  | "primary"
  | "secondary"
  | "danger"
  | "ghost";

export type ButtonSize =
  | "default"
  | "compact"
  | "icon";

export type ButtonProps =
  ButtonHTMLAttributes<HTMLButtonElement> & {
    variant?: ButtonVariant;
    size?: ButtonSize;
    leadingIcon?: ReactNode;
    trailingIcon?: ReactNode;
    fullWidth?: boolean;
  };

const variantClassNames: Record<
  ButtonVariant,
  string
> = {
  primary:
    "border-[var(--color-accent)] bg-[var(--color-accent)] text-[var(--color-accent-contrast)] hover:border-[var(--color-accent-hover)] hover:bg-[var(--color-accent-hover)]",

  secondary:
    "border-[var(--border-subtle)] bg-[var(--surface-raised)] text-[var(--text-base)] hover:border-[var(--border-hover)] hover:bg-[var(--surface-hover)]",

  danger:
    "border-[var(--color-danger-border)] bg-[var(--color-danger-soft)] text-[var(--color-danger)] hover:border-[var(--color-danger)] hover:text-[var(--color-danger-hover)]",

  ghost:
    "border-transparent bg-transparent text-[var(--text-muted)] hover:bg-[var(--surface-hover)] hover:text-[var(--text-base)]",
};

const sizeClassNames: Record<
  ButtonSize,
  string
> = {
  default:
    "h-[var(--control-height)] px-4 text-sm",

  compact:
    "h-[var(--control-height-compact)] px-3 text-[13px]",

  icon:
    "h-[var(--control-height-compact)] w-[var(--control-height-compact)] p-0",
};

export function Button({
  variant = "secondary",
  size = "default",
  leadingIcon,
  trailingIcon,
  fullWidth = false,
  className = "",
  children,
  type = "button",
  ...buttonProps
}: ButtonProps) {
  return (
    <button
      {...buttonProps}
      type={type}
      className={[
        "v7-motion-fast v7-pressable inline-flex shrink-0 items-center justify-center gap-2 rounded-[var(--control-radius)] border font-medium leading-none outline-none disabled:pointer-events-none disabled:opacity-40",
        variantClassNames[variant],
        sizeClassNames[size],
        fullWidth ? "w-full" : "",
        className,
      ].join(" ")}
    >
      {leadingIcon ? (
        <span
          className="grid h-4 w-4 shrink-0 place-items-center [&>svg]:h-4 [&>svg]:w-4"
          aria-hidden="true"
        >
          {leadingIcon}
        </span>
      ) : null}

      {children ? (
        <span className="truncate">
          {children}
        </span>
      ) : null}

      {trailingIcon ? (
        <span
          className="grid h-4 w-4 shrink-0 place-items-center [&>svg]:h-4 [&>svg]:w-4"
          aria-hidden="true"
        >
          {trailingIcon}
        </span>
      ) : null}
    </button>
  );
}
