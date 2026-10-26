import "@testing-library/jest-dom/vitest";
import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";

afterEach(() => {
  cleanup();
  localStorage.clear();
});

// Test helper: store a fake session the way authService.login does (fixture values only)
export const loginAs = ({ username = "fixture-user", role = "AUDITOR", permissions = [] } = {}) => {
  localStorage.setItem("sentinelcore_token", "fixture-token");
  localStorage.setItem("sentinelcore_role", role);
  localStorage.setItem("sentinelcore_permissions", JSON.stringify(permissions));
  localStorage.setItem("sentinelcore_user", JSON.stringify({ username, fullName: "Fixture", role }));
};
