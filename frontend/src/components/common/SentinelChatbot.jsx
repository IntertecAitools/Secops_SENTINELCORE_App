import React, { useState, useRef, useEffect } from "react";
import {
  Box,
  Paper,
  Typography,
  TextField,
  IconButton,
  Fab,
  Tooltip,
  Chip,
  Avatar,
  Badge,
  Divider,
  Button,
  Fade,
} from "@mui/material";
import {
  SmartToy as BotIcon,
  Send as SendIcon,
  Close as CloseIcon,
  Minimize as MinimizeIcon,
  OpenInFull as ExpandIcon,
  CloseFullscreen as CompressIcon,
  DeleteSweep as ClearIcon,
  ContentCopy as CopyIcon,
  Check as CheckIcon,
  AutoAwesome as SparklesIcon,
  Shield as AlertIcon,
  Security as SecurityIcon,
  BugReport as BugIcon,
  ArrowForward as ArrowIcon,
  Person as UserIcon,
} from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import API from "../../services/api";

const QUICK_PROMPTS = [
  { label: "🛡️ Analyze Active Alerts", query: "Analyze active security alerts and high risk threats" },
  { label: "⚡ Incident Response Protocol", query: "What is the standard incident containment response protocol?" },
  { label: "🔍 Explain CVSS Scoring", query: "Explain CVSS severity score thresholds" },
  { label: "📦 Patch Management Rules", query: "How do patch management policies work?" },
];

export default function SentinelChatbot() {
  const [isOpen, setIsOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [inputMessage, setInputMessage] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const [unreadCount, setUnreadCount] = useState(1);
  const [copiedId, setCopiedId] = useState(null);

  const navigate = useNavigate();
  const messagesEndRef = useRef(null);

  const [messages, setMessages] = useState([
    {
      id: "init-1",
      sender: "bot",
      text: "👋 Hello Agent! I am **Sentinel AI**, your enterprise cyber-security intelligence assistant.\n\nI can analyze threats, explain vulnerability scores, guide incident containment, and navigate system tools. Ask me anything!",
      timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    },
  ]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
      setUnreadCount(0);
    }
  }, [messages, isOpen]);

  const handleSendMessage = async (textToSend) => {
    const text = textToSend || inputMessage;
    if (!text.trim()) return;

    const userMsg = {
      id: `usr-${Date.now()}`,
      sender: "user",
      text: text.trim(),
      timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    };

    setMessages((prev) => [...prev, userMsg]);
    if (!textToSend) setInputMessage("");
    setIsTyping(true);

    try {
      // API call to Spring Boot backend endpoint
      const response = await API.post("/chatbot/query", { message: text });
      const botReplyData = response.data?.data;

      const botMsg = {
        id: `bot-${Date.now()}`,
        sender: "bot",
        text: botReplyData?.reply || "I have received your request and logged it in Sentinel AI matrix.",
        category: botReplyData?.category,
        actionUrl: botReplyData?.actionUrl,
        actionText: botReplyData?.actionText,
        timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
      };

      setMessages((prev) => [...prev, botMsg]);
    } catch (err) {
      console.warn("Backend AI chatbot offline, utilizing local fallback engine...", err);
      // Fallback local intelligence reasoning
      const botMsg = generateLocalResponse(text);
      setMessages((prev) => [...prev, botMsg]);
    } finally {
      setIsTyping(false);
    }
  };

  const generateLocalResponse = (query) => {
    const q = query.toLowerCase();
    let reply = "";
    let actionUrl = null;
    let actionText = null;

    if (q.includes("alert") || q.includes("threat")) {
      reply = "🛡️ **Security Alert Analysis:**\n\nSentinelCore tracks real-time intrusion detection and behavioral anomalies.\n\n• **Action Required**: Inspect unresolved Critical & High alerts on your dashboard.\n• **Remediation**: Isolate compromise vectors and review audit trail.";
      actionUrl = "/alerts";
      actionText = "Open Alerts Portal";
    } else if (q.includes("incident") || q.includes("response")) {
      reply = "⚡ **Incident Containment Protocol:**\n\n1. **Isolate**: Terminate affected API sessions & isolate network segment.\n2. **Triage**: Assign incident manager & assess blast radius.\n3. **Remediate**: Deploy security patches & cycle JWT credentials.";
      actionUrl = "/incidents";
      actionText = "View Incident Center";
    } else if (q.includes("vulnerab") || q.includes("cvss") || q.includes("cve")) {
      reply = "🔍 **CVSS 3.1 Severity Rating Matrix:**\n\n• **9.0 - 10.0 (Critical)**: Immediate hotfix execution.\n• **7.0 - 8.9 (High)**: Patch within 7 days.\n• **4.0 - 6.9 (Medium)**: Scheduled maintenance cycle.";
      actionUrl = "/vulnerabilities";
      actionText = "Check Vulnerability Index";
    } else if (q.includes("patch")) {
      reply = "📦 **Automated Patch Management:**\n\nSentinelCore evaluates pending security patches against active CVE databases to ensure minimum system downtime.";
      actionUrl = "/patch-management";
      actionText = "Go to Patch Management";
    } else {
      reply = `🤖 **Sentinel AI Security Insights:**\n\nI have evaluated your request regarding *"_${query}_"*.\n\nYou can query active **alerts**, **incidents**, **vulnerabilities**, **patches**, or **audit compliance** anytime!`;
    }

    return {
      id: `bot-${Date.now()}`,
      sender: "bot",
      text: reply,
      actionUrl,
      actionText,
      timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    };
  };

  const handleCopyText = (text, id) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const renderFormattedText = (text) => {
    // Simple markdown helper for bolding and bullet points
    const lines = text.split("\n");
    return lines.map((line, idx) => {
      let formattedLine = line;
      // Convert **bold**
      const parts = formattedLine.split(/(\*\*.*?\*\*)/g);
      return (
        <Typography
          key={idx}
          variant="body2"
          sx={{
            lineHeight: 1.5,
            mb: line === "" ? 1 : 0.4,
            wordBreak: "break-word",
          }}
        >
          {parts.map((part, pIdx) => {
            if (part.startsWith("**") && part.endsWith("**")) {
              return (
                <strong key={pIdx} style={{ color: "#38BDF8", fontWeight: 700 }}>
                  {part.slice(2, -2)}
                </strong>
              );
            }
            if (part.startsWith("*_") && part.endsWith("_*")) {
              return <em key={pIdx}>{part.slice(2, -2)}</em>;
            }
            return part;
          })}
        </Typography>
      );
    });
  };

  return (
    <>
      {/* Floating Trigger Button */}
      {!isOpen && (
        <Tooltip title="Sentinel AI Assistant" placement="left" arrow>
          <Badge
            badgeContent={unreadCount}
            color="error"
            overlap="circular"
            sx={{
              position: "fixed",
              bottom: 28,
              right: 28,
              zIndex: 1400,
              "& .MuiBadge-badge": {
                bgcolor: "#00F2FE",
                color: "#0F172A",
                fontWeight: 700,
                boxShadow: "0 0 10px #00F2FE",
              },
            }}
          >
            <Fab
              onClick={() => setIsOpen(true)}
              className="pulse-neon"
              sx={{
                width: 60,
                height: 60,
                background: "linear-gradient(135deg, #0284C7 0%, #0F172A 100%)",
                color: "#38BDF8",
                border: "1.5px solid rgba(56, 189, 248, 0.4)",
                boxShadow: "0 8px 32px rgba(2, 132, 199, 0.4)",
                transition: "all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)",
                "&:hover": {
                  transform: "scale(1.1) rotate(5deg)",
                  background: "linear-gradient(135deg, #0369A1 0%, #0F172A 100%)",
                  boxShadow: "0 12px 40px rgba(56, 189, 248, 0.6)",
                },
              }}
            >
              <BotIcon sx={{ fontSize: 32 }} />
            </Fab>
          </Badge>
        </Tooltip>
      )}

      {/* Interactive Chat Window */}
      <Fade in={isOpen} unmountOnExit mountOnEnter>
        <Paper
          elevation={24}
          sx={{
            position: "fixed",
            bottom: isExpanded ? 16 : 28,
            right: isExpanded ? 16 : 28,
            width: isExpanded ? "calc(100vw - 32px)" : { xs: "calc(100vw - 32px)", sm: 420 },
            maxWidth: isExpanded ? 1000 : 440,
            height: isExpanded ? "calc(100vh - 32px)" : { xs: "calc(100vh - 100px)", sm: 580 },
            maxHeight: 750,
            zIndex: 1500,
            borderRadius: 4,
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
            bgcolor: "rgba(15, 23, 42, 0.95)",
            backdropFilter: "blur(20px)",
            border: "1px solid rgba(56, 189, 248, 0.25)",
            boxShadow: "0 20px 60px rgba(0, 0, 0, 0.7), 0 0 30px rgba(56, 189, 248, 0.15)",
            transition: "all 0.3s cubic-bezier(0.16, 1, 0.3, 1)",
          }}
        >
          {/* Header */}
          <Box
            sx={{
              p: 2,
              display: "flex",
              alignItems: "center",
              justify: "space-between",
              background: "linear-gradient(90deg, rgba(15,23,42,0.9) 0%, rgba(30,41,59,0.9) 100%)",
              borderBottom: "1px solid rgba(255,255,255,0.08)",
            }}
          >
            <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
              <Avatar
                sx={{
                  bgcolor: "rgba(56, 189, 248, 0.15)",
                  color: "#38BDF8",
                  border: "1px solid rgba(56, 189, 248, 0.4)",
                  width: 42,
                  height: 42,
                }}
              >
                <BotIcon />
              </Avatar>
              <Box>
                <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                  <Typography variant="subtitle1" sx={{ fontWeight: 700, color: "#F8FAFC", fontSize: "0.95rem" }}>
                    Sentinel AI Assistant
                  </Typography>
                  <SparklesIcon sx={{ fontSize: 16, color: "#38BDF8" }} />
                </Box>
                <Box sx={{ display: "flex", alignItems: "center", gap: 0.8 }}>
                  <Box
                    sx={{
                      width: 8,
                      height: 8,
                      borderRadius: "50%",
                      bgcolor: "#22C55E",
                      boxShadow: "0 0 8px #22C55E",
                    }}
                  />
                  <Typography variant="caption" sx={{ color: "#94A3B8", fontSize: "0.75rem" }}>
                    Active • Cyber Threat Intelligence
                  </Typography>
                </Box>
              </Box>
            </Box>

            {/* Window Controls */}
            <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
              <Tooltip title="Clear chat">
                <IconButton
                  size="small"
                  onClick={() =>
                    setMessages([
                      {
                        id: `init-${Date.now()}`,
                        sender: "bot",
                        text: "Chat cleared. How can I assist you next, Agent?",
                        timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
                      },
                    ])
                  }
                  sx={{ color: "#94A3B8", "&:hover": { color: "#F8FAFC" } }}
                >
                  <ClearIcon fontSize="small" />
                </IconButton>
              </Tooltip>

              <Tooltip title={isExpanded ? "Restore" : "Expand"}>
                <IconButton
                  size="small"
                  onClick={() => setIsExpanded(!isExpanded)}
                  sx={{ color: "#94A3B8", "&:hover": { color: "#F8FAFC" }, display: { xs: "none", sm: "inline-flex" } }}
                >
                  {isExpanded ? <CompressIcon fontSize="small" /> : <ExpandIcon fontSize="small" />}
                </IconButton>
              </Tooltip>

              <Tooltip title="Minimize">
                <IconButton
                  size="small"
                  onClick={() => setIsOpen(false)}
                  sx={{ color: "#94A3B8", "&:hover": { color: "#EF4444" } }}
                >
                  <CloseIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </Box>
          </Box>

          {/* Quick Action Prompt Chips */}
          <Box
            sx={{
              px: 2,
              py: 1,
              bgcolor: "rgba(30, 41, 59, 0.4)",
              borderBottom: "1px solid rgba(255,255,255,0.05)",
              display: "flex",
              gap: 1,
              overflowX: "auto",
              whiteSpace: "nowrap",
              "&::-webkit-scrollbar": { height: 4 },
            }}
          >
            {QUICK_PROMPTS.map((prompt, idx) => (
              <Chip
                key={idx}
                label={prompt.label}
                size="small"
                onClick={() => handleSendMessage(prompt.query)}
                sx={{
                  bgcolor: "rgba(56, 189, 248, 0.08)",
                  color: "#38BDF8",
                  borderColor: "rgba(56, 189, 248, 0.25)",
                  borderWidth: 1,
                  borderStyle: "solid",
                  fontSize: "0.75rem",
                  cursor: "pointer",
                  transition: "all 0.2s ease",
                  "&:hover": {
                    bgcolor: "rgba(56, 189, 248, 0.2)",
                    transform: "translateY(-1px)",
                  },
                }}
              />
            ))}
          </Box>

          {/* Message List */}
          <Box
            sx={{
              flex: 1,
              p: 2,
              overflowY: "auto",
              display: "flex",
              flexDirection: "column",
              gap: 2,
            }}
          >
            {messages.map((msg) => {
              const isUser = msg.sender === "user";
              return (
                <Box
                  key={msg.id}
                  sx={{
                    display: "flex",
                    justifyContent: isUser ? "flex-end" : "flex-start",
                    alignItems: "flex-start",
                    gap: 1.2,
                  }}
                >
                  {!isUser && (
                    <Avatar
                      sx={{
                        width: 32,
                        height: 32,
                        bgcolor: "rgba(56, 189, 248, 0.15)",
                        color: "#38BDF8",
                        border: "1px solid rgba(56, 189, 248, 0.3)",
                        fontSize: "0.85rem",
                      }}
                    >
                      <BotIcon fontSize="small" />
                    </Avatar>
                  )}

                  <Box
                    sx={{
                      maxWidth: "82%",
                      position: "relative",
                      group: "msg-box",
                    }}
                  >
                    <Paper
                      elevation={0}
                      sx={{
                        p: 1.8,
                        borderRadius: isUser ? "16px 16px 4px 16px" : "16px 16px 16px 4px",
                        bgcolor: isUser ? "#0284C7" : "rgba(30, 41, 59, 0.8)",
                        color: "#F8FAFC",
                        border: isUser ? "none" : "1px solid rgba(255, 255, 255, 0.08)",
                        boxShadow: isUser
                          ? "0 4px 14px rgba(2, 132, 199, 0.3)"
                          : "0 4px 14px rgba(0, 0, 0, 0.2)",
                      }}
                    >
                      {renderFormattedText(msg.text)}

                      {/* Action Button inside Bot Message */}
                      {msg.actionUrl && (
                        <Box sx={{ mt: 1.5, pt: 1, borderTop: "1px solid rgba(255,255,255,0.1)" }}>
                          <Button
                            size="small"
                            variant="outlined"
                            endIcon={<ArrowIcon fontSize="small" />}
                            onClick={() => navigate(msg.actionUrl)}
                            sx={{
                              borderColor: "rgba(56, 189, 248, 0.5)",
                              color: "#38BDF8",
                              fontSize: "0.75rem",
                              py: 0.4,
                              px: 1.2,
                              borderRadius: 2,
                              textTransform: "none",
                              "&:hover": {
                                bgcolor: "rgba(56, 189, 248, 0.15)",
                                borderColor: "#38BDF8",
                              },
                            }}
                          >
                            {msg.actionText || "View Details"}
                          </Button>
                        </Box>
                      )}
                    </Paper>

                    {/* Metadata & Copy button */}
                    <Box
                      sx={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: isUser ? "flex-end" : "space-between",
                        mt: 0.5,
                        px: 0.5,
                      }}
                    >
                      <Typography variant="caption" sx={{ color: "#64748B", fontSize: "0.68rem" }}>
                        {msg.timestamp}
                      </Typography>

                      {!isUser && (
                        <IconButton
                          size="small"
                          onClick={() => handleCopyText(msg.text, msg.id)}
                          sx={{ color: "#64748B", p: 0.2, "&:hover": { color: "#38BDF8" } }}
                        >
                          {copiedId === msg.id ? (
                            <CheckIcon sx={{ fontSize: 13, color: "#22C55E" }} />
                          ) : (
                            <CopyIcon sx={{ fontSize: 13 }} />
                          )}
                        </IconButton>
                      )}
                    </Box>
                  </Box>

                  {isUser && (
                    <Avatar
                      sx={{
                        width: 32,
                        height: 32,
                        bgcolor: "rgba(2, 132, 199, 0.3)",
                        color: "#F8FAFC",
                        border: "1px solid rgba(2, 132, 199, 0.5)",
                      }}
                    >
                      <UserIcon fontSize="small" />
                    </Avatar>
                  )}
                </Box>
              );
            })}

            {/* Typing Indicator */}
            {isTyping && (
              <Box sx={{ display: "flex", alignItems: "center", gap: 1.2 }}>
                <Avatar
                  sx={{
                    width: 32,
                    height: 32,
                    bgcolor: "rgba(56, 189, 248, 0.15)",
                    color: "#38BDF8",
                  }}
                >
                  <BotIcon fontSize="small" />
                </Avatar>
                <Paper
                  sx={{
                    px: 2,
                    py: 1.2,
                    borderRadius: "16px 16px 16px 4px",
                    bgcolor: "rgba(30, 41, 59, 0.8)",
                    border: "1px solid rgba(255, 255, 255, 0.08)",
                    display: "flex",
                    alignItems: "center",
                    gap: 0.8,
                  }}
                >
                  <Typography variant="caption" sx={{ color: "#94A3B8", fontStyle: "italic" }}>
                    Sentinel AI is analyzing threat vectors...
                  </Typography>
                </Paper>
              </Box>
            )}

            <div ref={messagesEndRef} />
          </Box>

          <Divider sx={{ borderColor: "rgba(255, 255, 255, 0.08)" }} />

          {/* Footer Input Form */}
          <Box
            component="form"
            onSubmit={(e) => {
              e.preventDefault();
              handleSendMessage();
            }}
            sx={{
              p: 1.5,
              bgcolor: "rgba(15, 23, 42, 0.98)",
              display: "flex",
              alignItems: "center",
              gap: 1,
            }}
          >
            <TextField
              fullWidth
              size="small"
              placeholder="Ask Sentinel AI about threats, CVEs, or tools..."
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              disabled={isTyping}
              sx={{
                "& .MuiOutlinedInput-root": {
                  borderRadius: 3,
                  bgcolor: "rgba(30, 41, 59, 0.6)",
                  color: "#F8FAFC",
                  fontSize: "0.88rem",
                  "& fieldset": {
                    borderColor: "rgba(255, 255, 255, 0.1)",
                  },
                  "&:hover fieldset": {
                    borderColor: "rgba(56, 189, 248, 0.4)",
                  },
                  "&.Mui-focused fieldset": {
                    borderColor: "#38BDF8",
                  },
                },
                "& .MuiInputBase-input::placeholder": {
                  color: "#64748B",
                  opacity: 1,
                },
              }}
            />
            <IconButton
              type="submit"
              disabled={!inputMessage.trim() || isTyping}
              sx={{
                bgcolor: "#0284C7",
                color: "#F8FAFC",
                p: 1.2,
                borderRadius: 2.5,
                transition: "all 0.2s ease",
                "&:hover": {
                  bgcolor: "#0369A1",
                  transform: "scale(1.05)",
                },
                "&.Mui-disabled": {
                  bgcolor: "rgba(255, 255, 255, 0.05)",
                  color: "rgba(255, 255, 255, 0.2)",
                },
              }}
            >
              <SendIcon fontSize="small" />
            </IconButton>
          </Box>
        </Paper>
      </Fade>
    </>
  );
}
