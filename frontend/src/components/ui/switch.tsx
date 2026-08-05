type SwitchVisualProps = {
  checked: boolean;
};

export function SwitchVisual({
  checked,
}: SwitchVisualProps) {
  const state = checked ? "checked" : "unchecked";

  return (
    <span
      aria-hidden="true"
      className="v7-switch-root"
      data-state={state}
    >
      <span
        className="v7-switch-thumb"
        data-state={state}
      />
    </span>
  );
}
