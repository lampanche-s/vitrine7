import {
  defineConfig,
} from "vitest/config";

export default defineConfig({
  test: {
    environment: "node",

    include: [
      "src/**/*.test.ts",
    ],

    coverage: {
      provider: "v8",

      reporter: [
        "text",
        "html",
      ],

      include: [
        "src/entities/**/*.rules.ts",
        "src/features/**/state/*.reducer.ts",
        "src/features/access/access.rules.ts",
      ],

      exclude: [
        "src/**/*.test.ts",
        "src/**/index.ts",
        "src/**/*.types.ts",
      ],
    },
  },
});
