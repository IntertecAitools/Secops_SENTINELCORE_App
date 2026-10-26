import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import ProtectedRoute from "./ProtectedRoute";
import RoleProtectedRoute from "./RoleProtectedRoute";
import { loginAs } from "../../test/setup";

const renderAt = (element) =>
  render(
    <MemoryRouter initialEntries={["/secure"]}>
      <Routes>
        <Route path="/login" element={<div>Login Page</div>} />
        <Route path="/secure" element={element} />
      </Routes>
    </MemoryRouter>
  );

describe("existing route guards", () => {
  it("redirects unauthenticated users to login", () => {
    renderAt(<ProtectedRoute><div>Secret</div></ProtectedRoute>);
    expect(screen.getByText("Login Page")).toBeInTheDocument();
    expect(screen.queryByText("Secret")).not.toBeInTheDocument();
  });

  it("renders protected content for authenticated users", () => {
    loginAs();
    renderAt(<ProtectedRoute><div>Secret</div></ProtectedRoute>);
    expect(screen.getByText("Secret")).toBeInTheDocument();
  });

  it("shows access denied for roles outside the allow list", () => {
    loginAs({ role: "ASSET_MANAGER" });
    renderAt(
      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "AUDITOR"]}>
        <div>Cloud Page</div>
      </RoleProtectedRoute>
    );
    expect(screen.getByText("403 - Access Denied")).toBeInTheDocument();
    expect(screen.queryByText("Cloud Page")).not.toBeInTheDocument();
  });

  it("allows roles in the allow list", () => {
    loginAs({ role: "AUDITOR" });
    renderAt(
      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "AUDITOR"]}>
        <div>Cloud Page</div>
      </RoleProtectedRoute>
    );
    expect(screen.getByText("Cloud Page")).toBeInTheDocument();
  });
});
