type NumericIdEntity = {
  id: number;
};

export function getNextNumericId<T extends NumericIdEntity>(
  items: readonly T[]
): number {
  return (
    items.reduce(
      (highestId, item) => Math.max(highestId, item.id),
      0
    ) + 1
  );
}
