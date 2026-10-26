import { describe, expect, it } from "vitest";
import API, { apiErrorMessage } from "./api";
import { loginAs } from "../test/setup";

const rejected = API.interceptors.response.handlers[0].rejected;

describe("API response interceptor", () => {
  it("logs out on 401 for non-auth endpoints", async () => {
    window.history.pushState({}, "", "/login");
    loginAs();
    await expect(rejected({ response: { status: 401 }, config: { url: "/assets" } })).rejects.toBeTruthy();
    expect(localStorage.getItem("sentinelcore_token")).toBeNull();
    expect(localStorage.getItem("sentinelcore_permissions")).toBeNull();
  });

  it("keeps the session on 403 so pages can show a permission error", async () => {
    loginAs();
    await expect(rejected({ response: { status: 403 }, config: { url: "/audit-logs" } })).rejects.toBeTruthy();
    expect(localStorage.getItem("sentinelcore_token")).toBe("fixture-token");
  });

  it("does not treat a failed login (401 from /auth) as an expired session", async () => {
    window.history.pushState({}, "", "/");
    loginAs();
    await expect(rejected({ response: { status: 401 }, config: { url: "/auth/login" } })).rejects.toBeTruthy();
    expect(localStorage.getItem("sentinelcore_token")).toBe("fixture-token");
    expect(window.location.pathname).toBe("/");
  });
});

describe("apiErrorMessage", () => {
  it("prefers the server message and has a permission fallback", () => {
    expect(apiErrorMessage({ response: { status: 409, data: { message: "Conflict detail" } } })).toBe("Conflict detail");
    expect(apiErrorMessage({ response: { status: 403, data: {} } })).toMatch(/permission/i);
    expect(apiErrorMessage({}, "fallback")).toBe("fallback");
  });
});
