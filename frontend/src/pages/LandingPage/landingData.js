import ComputerIcon from "@mui/icons-material/Computer";
import TravelExploreIcon from "@mui/icons-material/TravelExplore";
import ShieldIcon from "@mui/icons-material/Shield";
import CloudIcon from "@mui/icons-material/Cloud";
import VisibilityIcon from "@mui/icons-material/Visibility";
import PsychologyIcon from "@mui/icons-material/Psychology";
import LinkIcon from "@mui/icons-material/Link";
import VerifiedUserIcon from "@mui/icons-material/VerifiedUser";
import RadarIcon from "@mui/icons-material/Radar";
import HubIcon from "@mui/icons-material/Hub";
import CloudSyncIcon from "@mui/icons-material/CloudSync";

export const NAV_LINKS = [
  { label: "Platform", target: "preview" },
  { label: "Security", target: "security" },
  { label: "Enterprise", target: "why" },
  { label: "Pricing", target: "get-started" },
];

export const COVERAGE_CHIPS = [
  "Endpoint Security",
  "Threat Intelligence",
  "Security Operations",
  "Cloud Security",
];

export const CAPABILITIES = [
  {
    icon: ComputerIcon,
    title: "Endpoint Security",
    capabilities: ["Configuration Assessment", "Malware Detection", "File Integrity Monitoring"],
    description:
      "Monitor endpoint security signals, identify configuration weaknesses, and investigate suspicious file changes.",
  },
  {
    icon: TravelExploreIcon,
    title: "Threat Intelligence",
    capabilities: ["Threat Hunting", "Log Data Analysis", "Vulnerability Detection"],
    description:
      "Explore security events, investigate suspicious activity, and understand vulnerability findings using available evidence.",
  },
  {
    icon: ShieldIcon,
    title: "Security Operations",
    capabilities: ["Incident Response", "Regulatory Compliance", "IT Hygiene"],
    description:
      "Organize incident investigations, review compliance-related findings, and identify operational security gaps.",
  },
  {
    icon: CloudIcon,
    title: "Cloud Security",
    capabilities: ["Container Security", "Posture Management", "Workload Protection"],
    description:
      "Bring supported cloud and workload findings into a consistent security investigation workflow.",
  },
];

export const BENEFITS = [
  {
    icon: VisibilityIcon,
    title: "Unified Security Visibility",
    points: [
      "Bring together findings from supported security sources.",
      "Make source and timestamp information easy to identify.",
    ],
  },
  {
    icon: PsychologyIcon,
    title: "AI-Assisted Investigations",
    points: [
      "Help analysts understand alerts and relevant evidence.",
      "Present explanations and suggested investigative checks.",
    ],
  },
  {
    icon: LinkIcon,
    title: "Evidence-Linked Insights",
    points: [
      "Keep explanations connected to supporting events and findings.",
      "Clearly distinguish known facts from assumptions and missing evidence.",
    ],
  },
  {
    icon: VerifiedUserIcon,
    title: "Analyst-Controlled Response",
    points: [
      "Help analysts review and organize investigation steps.",
      "Keep consequential response actions under authorized human control.",
    ],
  },
];

export const STAGES = [
  {
    label: "Stage 01",
    title: "Collect",
    description: "Collect security findings from configured and supported sources.",
  },
  {
    label: "Stage 02",
    title: "Analyze",
    description:
      "Organize alerts and identify potentially related events where evidence supports a relationship.",
  },
  {
    label: "Stage 03",
    title: "Investigate",
    description: "Review the evidence, timeline, context, and AI-assisted explanations.",
  },
  {
    label: "Stage 04",
    title: "Decide and Respond",
    description: "Help authorized analysts determine the next steps and record their decisions.",
  },
];

export const PREVIEW = {
  sidebar: ["Overview", "Alerts", "Investigations", "Assets", "Cases"],
  activeItem: "Investigations",
  alertTitle: "Suspicious Privilege Escalation",
  timeline: [
    { time: "13:58Z", event: "Unexpected sudo membership change detected on workspace host." },
    { time: "14:02Z", event: "Privileged command executed outside of the usual session window." },
    { time: "14:05Z", event: "Configuration change recorded against the endpoint baseline." },
    { time: "14:11Z", event: "Alert grouped into an investigation with linked context." },
  ],
  evidence: [
    { label: "Log event", text: "Elevated group membership change" },
    { label: "Config diff", text: "Baseline drift on sudoers file" },
    { label: "Session", text: "Off-hours shell session on recorded host" },
  ],
  summary:
    "The recorded events are consistent with an unexpected privilege escalation attempt. Evidence linking is illustrative in this preview: available facts, assumptions, and missing evidence are separated once connected data sources are configured.",
  nextChecks: [
    "Verify the modified membership entry against the endpoint baseline",
    "Review access records for the privileged group",
    "Collect endpoint configuration state for review",
  ],
};

export const INTEGRATIONS = [
  {
    icon: RadarIcon,
    name: "AWS GuardDuty",
    status: "Planned",
    description:
      "Designed to collect managed threat detection findings from AWS accounts once configured.",
  },
  {
    icon: HubIcon,
    name: "AWS Security Hub",
    status: "Planned",
    description:
      "Designed to aggregate consolidated security findings and compliance posture into investigations.",
  },
  {
    icon: CloudSyncIcon,
    name: "AWS CloudTrail",
    status: "Planned",
    description:
      "Designed to bring account activity and API audit trails into the investigation timeline.",
  },
];

export const FOOTER_LINKS = {
  sections: [
    {
      heading: "Product",
      items: [
        { label: "Platform", target: "preview" },
        { label: "Security", target: "security" },
        { label: "Enterprise", target: "why" },
        { label: "Pricing", target: "get-started" },
      ],
    },
  ],
  mutedItems: ["Contact", "Privacy", "Terms"],
};
