import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import CloudSecurity from "./CloudSecurity";
import { loginAs } from "../../test/setup";
import * as cloudService from "../../services/cloudService";

// All data below are explicitly labelled test fixtures, not real cloud data.
vi.mock("../../services/cloudService", () => ({
  getCloudCapabilities: vi.fn(),
  getCloudAccounts: vi.fn(),
  createCloudAccount: vi.fn(),
  updateCloudAccount: vi.fn(),
  verifyCloudAccount: vi.fn(),
  runCloudScan: vi.fn(),
  getCloudScans: vi.fn(),
  getCloudResources: vi.fn(),
  getCloudFindings: vi.fn(),
  recheckCloudFinding: vi.fn(),
  getRemediations: vi.fn(),
  createRemediation: vi.fn(),
  previewRemediation: vi.fn(),
  approveRemediation: vi.fn(),
  rejectRemediation: vi.fn(),
  cancelRemediation: vi.fn(),
  executeRemediation: vi.fn(),
  verifyRemediation: vi.fn(),
  newIdempotencyKey: vi.fn(() => "fixture-idempotency-key"),
}));

const CAPABILITIES = {
  supportedProviders: ["AWS"],
  knownButUnsupportedProviders: ["AZURE", "GCP"],
  awsIntegrationEnabled: false,
  remediationExecutionEnabled: false,
  remediationActions: ["S3_BLOCK_PUBLIC_ACCESS"],
};

const FINDING = {
  id: 7,
  cloudAccountId: 1,
  cloudAccountName: "Fixture Account",
  findingType: "S3_PUBLIC_ACCESS_BLOCK_DISABLED",
  severity: "MEDIUM",
  status: "OPEN",
  resourceType: "s3:bucket",
  resourceId: "arn:aws:s3:::fixture-bucket",
  region: "us-east-1",
  title: "S3 bucket Block Public Access is not fully enabled: fixture-bucket",
  observedFact: "GetPublicAccessBlock returned NoSuchPublicAccessBlockConfiguration for the bucket.",
  inferredRisk: "An ACL or bucket policy change can make objects public.",
  evidence: { configured: "false" },
  source: "AWS S3 GetPublicAccessBlock",
  supportedRemediation: "S3_BLOCK_PUBLIC_ACCESS",
  lastObservedAt: "2026-10-01T12:00:00",
};

const remediation = (overrides = {}) => ({
  id: 11,
  cloudAccountName: "Fixture Account",
  findingId: 7,
  findingTitle: FINDING.title,
  actionType: "S3_BLOCK_PUBLIC_ACCESS",
  resourceType: "s3:bucket",
  resourceId: "arn:aws:s3:::fixture-bucket",
  region: "us-east-1",
  requestHash: "a".repeat(64),
  status: "PENDING_APPROVAL",
  requestedBy: "requester-user",
  requestedAt: "2026-10-01T12:00:00",
  executionAttempts: 0,
  ...overrides,
});

const page = (content) => ({ content, totalElements: content.length });

beforeEach(() => {
  vi.mocked(cloudService.getCloudCapabilities).mockResolvedValue(CAPABILITIES);
  vi.mocked(cloudService.getCloudAccounts).mockResolvedValue([]);
  vi.mocked(cloudService.getCloudFindings).mockResolvedValue(page([FINDING]));
  vi.mocked(cloudService.getRemediations).mockResolvedValue(page([]));
});

describe("CloudSecurity page", () => {
  it("explains disabled integration and shows the empty account state", async () => {
    loginAs({ role: "AUDITOR", permissions: ["CLOUD_ACCOUNT_VIEW"] });
    render(<CloudSecurity />);

    expect(await screen.findByText(/AWS integration is disabled/)).toBeInTheDocument();
    expect(screen.getByText(/Remediation execution is disabled/)).toBeInTheDocument();
    expect(screen.getByText(/Not yet supported: AZURE, GCP/)).toBeInTheDocument();
    expect(await screen.findByText("No cloud accounts connected.")).toBeInTheDocument();
    // Auditors have no management permission, so no connect button
    expect(screen.queryByText("Connect Account")).not.toBeInTheDocument();
  });

  it("shows an access message when the API returns 403", async () => {
    loginAs({ role: "AUDITOR" });
    vi.mocked(cloudService.getCloudCapabilities).mockRejectedValue({ response: { status: 403, data: {} } });
    render(<CloudSecurity />);
    expect(await screen.findByText(/does not have access to cloud security data/)).toBeInTheDocument();
  });

  it("keeps the page usable when loading accounts fails", async () => {
    loginAs({ role: "ADMIN", permissions: ["CLOUD_ACCOUNT_VIEW", "CLOUD_ACCOUNT_MANAGE"] });
    vi.mocked(cloudService.getCloudAccounts).mockRejectedValue({ response: { status: 502, data: { message: "Upstream failure" } } });
    render(<CloudSecurity />);
    expect(await screen.findByText("Upstream failure")).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Findings" })).toBeInTheDocument();
  });

  it("shows finding evidence and offers remediation only with permission", async () => {
    loginAs({ role: "AUDITOR", permissions: ["CLOUD_ACCOUNT_VIEW"] });
    const { unmount } = render(<CloudSecurity />);
    fireEvent.click(await screen.findByRole("tab", { name: "Findings" }));
    fireEvent.click(await screen.findByText(FINDING.title));

    const dialog = await screen.findByRole("dialog");
    expect(within(dialog).getByText("Observed (from provider API)")).toBeInTheDocument();
    expect(within(dialog).getByText(FINDING.observedFact)).toBeInTheDocument();
    expect(within(dialog).getByText(FINDING.inferredRisk)).toBeInTheDocument();
    expect(within(dialog).queryByText("Request Remediation")).not.toBeInTheDocument();
    unmount();

    loginAs({ role: "VULNERABILITY_MANAGER", permissions: ["CLOUD_ACCOUNT_VIEW", "REMEDIATION_REQUEST"] });
    vi.mocked(cloudService.createRemediation).mockResolvedValue(remediation());
    render(<CloudSecurity />);
    fireEvent.click(await screen.findByRole("tab", { name: "Findings" }));
    fireEvent.click(await screen.findByText(FINDING.title));
    fireEvent.click(await screen.findByText("Request Remediation"));
    fireEvent.click(await screen.findByText("Submit for Approval"));

    await waitFor(() => expect(cloudService.createRemediation).toHaveBeenCalledWith(
      { findingId: 7, actionType: "S3_BLOCK_PUBLIC_ACCESS", justification: "" },
      "fixture-idempotency-key"
    ));
  });

  it("does not let the requester approve their own request", async () => {
    loginAs({ username: "requester-user", role: "VULNERABILITY_MANAGER",
      permissions: ["CLOUD_ACCOUNT_VIEW", "REMEDIATION_REQUEST", "REMEDIATION_APPROVE"] });
    vi.mocked(cloudService.getRemediations).mockResolvedValue(page([remediation()]));
    render(<CloudSecurity />);
    fireEvent.click(await screen.findByRole("tab", { name: "Remediations" }));
    fireEvent.click(await screen.findByText("arn:aws:s3:::fixture-bucket"));

    const dialog = await screen.findByRole("dialog");
    expect(within(dialog).getByText(/another approver must review it/)).toBeInTheDocument();
    expect(within(dialog).queryByRole("button", { name: "Approve" })).not.toBeInTheDocument();
  });

  it("approves with the reviewed request hash", async () => {
    loginAs({ username: "approver-user", role: "SUPER_ADMIN",
      permissions: ["CLOUD_ACCOUNT_VIEW", "REMEDIATION_APPROVE"] });
    vi.mocked(cloudService.getRemediations).mockResolvedValue(page([remediation()]));
    vi.mocked(cloudService.approveRemediation).mockResolvedValue(remediation({ status: "APPROVED", decidedBy: "approver-user" }));
    render(<CloudSecurity />);
    fireEvent.click(await screen.findByRole("tab", { name: "Remediations" }));
    fireEvent.click(await screen.findByText("arn:aws:s3:::fixture-bucket"));
    fireEvent.click(await screen.findByRole("button", { name: "Approve" }));

    await waitFor(() => expect(cloudService.approveRemediation).toHaveBeenCalledWith(11, "a".repeat(64), ""));
  });

  it("requires typing the bucket name before execution", async () => {
    loginAs({ username: "executor", role: "VULNERABILITY_MANAGER",
      permissions: ["CLOUD_ACCOUNT_VIEW", "REMEDIATION_EXECUTE"] });
    vi.mocked(cloudService.getCloudCapabilities).mockResolvedValue({ ...CAPABILITIES, awsIntegrationEnabled: true, remediationExecutionEnabled: true });
    vi.mocked(cloudService.getRemediations).mockResolvedValue(page([remediation({ status: "APPROVED", decidedBy: "approver-user" })]));
    vi.mocked(cloudService.executeRemediation).mockResolvedValue(remediation({ status: "VERIFIED" }));
    render(<CloudSecurity />);
    fireEvent.click(await screen.findByRole("tab", { name: "Remediations" }));
    fireEvent.click(await screen.findByText("arn:aws:s3:::fixture-bucket"));
    fireEvent.click(await screen.findByRole("button", { name: "Execute" }));

    const confirm = await screen.findByRole("button", { name: "Execute Change" });
    expect(confirm).toBeDisabled();
    fireEvent.change(screen.getByLabelText("Resource name"), { target: { value: "wrong-bucket" } });
    expect(confirm).toBeDisabled();
    fireEvent.change(screen.getByLabelText("Resource name"), { target: { value: "fixture-bucket" } });
    expect(confirm).toBeEnabled();
    fireEvent.click(confirm);

    await waitFor(() => expect(cloudService.executeRemediation).toHaveBeenCalledWith(11, "fixture-bucket"));
  });

  it("disables execution when the server kill switch is off", async () => {
    loginAs({ username: "executor", role: "VULNERABILITY_MANAGER",
      permissions: ["CLOUD_ACCOUNT_VIEW", "REMEDIATION_EXECUTE"] });
    vi.mocked(cloudService.getRemediations).mockResolvedValue(page([remediation({ status: "APPROVED", decidedBy: "approver-user" })]));
    render(<CloudSecurity />);
    fireEvent.click(await screen.findByRole("tab", { name: "Remediations" }));
    fireEvent.click(await screen.findByText("arn:aws:s3:::fixture-bucket"));

    expect(await screen.findByRole("button", { name: "Execute" })).toBeDisabled();
    expect(screen.getByText("Execution is disabled by server configuration.")).toBeInTheDocument();
  });
});
