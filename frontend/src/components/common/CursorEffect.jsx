import React, { useEffect, useRef, useState } from "react";
import { Box, Tooltip, IconButton, Paper, Stack } from "@mui/material";
import FlashOnIcon from "@mui/icons-material/FlashOn";
import ScatterPlotIcon from "@mui/icons-material/ScatterPlot";
import GrainIcon from "@mui/icons-material/Grain";
import OffIcon from "@mui/icons-material/Block";

function CursorEffect() {
  const canvasRef = useRef(null);
  const [mode, setMode] = useState("thunder"); // "thunder" | "dots" | "matrix" | "off"
  const mouseRef = useRef({ x: -1000, y: -1000, lastX: -1000, lastY: -1000, speed: 0 });
  const particlesRef = useRef([]);
  const boltsRef = useRef([]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext("2d");
    let animationFrameId;

    const resizeCanvas = () => {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    };

    resizeCanvas();
    window.addEventListener("resize", resizeCanvas);

    const handleMouseMove = (e) => {
      const { clientX, clientY } = e;
      const dx = clientX - mouseRef.current.x;
      const dy = clientY - mouseRef.current.y;
      const dist = Math.sqrt(dx * dx + dy * dy);

      mouseRef.current.lastX = mouseRef.current.x;
      mouseRef.current.lastY = mouseRef.current.y;
      mouseRef.current.x = clientX;
      mouseRef.current.y = clientY;
      mouseRef.current.speed = dist;

      // Spawn particles/thunder on move based on active mode
      if (mode === "thunder") {
        spawnThunderSparks(clientX, clientY, dist);
      } else if (mode === "dots") {
        spawnDots(clientX, clientY, dist);
      } else if (mode === "matrix") {
        spawnMatrixNodes(clientX, clientY);
      }
    };

    const handleTouchMove = (e) => {
      if (e.touches.length > 0) {
        handleMouseMove(e.touches[0]);
      }
    };

    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("touchmove", handleTouchMove);

    // --- MODE 1: ELECTRIC THUNDER SPARKS ---
    const spawnThunderSparks = (x, y, speed) => {
      const sparkCount = Math.min(Math.floor(speed * 0.4) + 3, 10);
      for (let i = 0; i < sparkCount; i++) {
        const angle = Math.random() * Math.PI * 2;
        const velocity = Math.random() * 4 + 2;
        particlesRef.current.push({
          x,
          y,
          vx: Math.cos(angle) * velocity,
          vy: Math.sin(angle) * velocity,
          life: 1.0,
          decay: Math.random() * 0.05 + 0.03,
          size: Math.random() * 3 + 2,
          color: Math.random() > 0.5 ? "#00E5FF" : Math.random() > 0.3 ? "#00C853" : "#7C4DFF",
        });
      }

      // Occasional lightning bolt arc branch
      if (Math.random() < 0.4 || speed > 15) {
        const angle = Math.random() * Math.PI * 2;
        const length = Math.random() * 60 + 30;
        const targetX = x + Math.cos(angle) * length;
        const targetY = y + Math.sin(angle) * length;

        boltsRef.current.push({
          startX: x,
          startY: y,
          endX: targetX,
          endY: targetY,
          segments: generateLightningPath(x, y, targetX, targetY),
          life: 1.0,
          decay: 0.12,
          color: Math.random() > 0.3 ? "#00E5FF" : "#00C853",
        });
      }
    };

    const generateLightningPath = (x1, y1, x2, y2) => {
      const segments = [{ x: x1, y: y1 }];
      const steps = Math.floor(Math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2) / 12) + 2;
      let currX = x1;
      let currY = y1;

      for (let i = 1; i < steps; i++) {
        const t = i / steps;
        const baseX = x1 + (x2 - x1) * t;
        const baseY = y1 + (y2 - y1) * t;
        const offset = (Math.random() - 0.5) * 16;

        const angle = Math.atan2(y2 - y1, x2 - x1) + Math.PI / 2;
        currX = baseX + Math.cos(angle) * offset;
        currY = baseY + Math.sin(angle) * offset;

        segments.push({ x: currX, y: currY });
      }
      segments.push({ x: x2, y: y2 });
      return segments;
    };

    // --- MODE 2: GROUP OF DOTS SWARM ---
    const spawnDots = (x, y, speed) => {
      const dotCount = Math.min(Math.floor(speed * 0.3) + 2, 8);
      for (let i = 0; i < dotCount; i++) {
        const angle = Math.random() * Math.PI * 2;
        const dist = Math.random() * 15;
        particlesRef.current.push({
          x: x + Math.cos(angle) * dist,
          y: y + Math.sin(angle) * dist,
          vx: (Math.random() - 0.5) * 2,
          vy: (Math.random() - 0.5) * 2 - 0.5,
          life: 1.0,
          decay: Math.random() * 0.02 + 0.015,
          size: Math.random() * 6 + 3,
          color: ["#00C853", "#00E5FF", "#FF4081", "#FFD600"][Math.floor(Math.random() * 4)],
        });
      }
    };

    // --- MODE 3: CYBER MATRIX NODES ---
    const spawnMatrixNodes = (x, y) => {
      if (particlesRef.current.length < 35 && Math.random() < 0.3) {
        particlesRef.current.push({
          x: x + (Math.random() - 0.5) * 60,
          y: y + (Math.random() - 0.5) * 60,
          vx: (Math.random() - 0.5) * 1.5,
          vy: (Math.random() - 0.5) * 1.5,
          life: 1.0,
          decay: 0.01,
          size: Math.random() * 4 + 2,
          color: "#00C853",
        });
      }
    };

    // --- MAIN ANIMATION LOOP ---
    const render = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      if (mode === "off") {
        animationFrameId = requestAnimationFrame(render);
        return;
      }

      const { x: mx, y: my } = mouseRef.current;

      if (mode === "thunder") {
        // Draw Lightning Bolts
        boltsRef.current.forEach((bolt, idx) => {
          ctx.beginPath();
          ctx.moveTo(bolt.segments[0].x, bolt.segments[0].y);
          for (let i = 1; i < bolt.segments.length; i++) {
            ctx.lineTo(bolt.segments[i].x, bolt.segments[i].y);
          }
          ctx.strokeStyle = bolt.color;
          ctx.lineWidth = bolt.life * 2.5;
          ctx.shadowBlur = 15;
          ctx.shadowColor = bolt.color;
          ctx.stroke();

          bolt.life -= bolt.decay;
          if (bolt.life <= 0) {
            boltsRef.current.splice(idx, 1);
          }
        });

        // Draw Core Energy Aura around mouse pointer
        if (mx > 0 && my > 0) {
          ctx.save();
          ctx.beginPath();
          ctx.arc(mx, my, 8, 0, Math.PI * 2);
          ctx.fillStyle = "#00E5FF";
          ctx.shadowBlur = 20;
          ctx.shadowColor = "#00E5FF";
          ctx.fill();

          ctx.beginPath();
          ctx.arc(mx, my, 3, 0, Math.PI * 2);
          ctx.fillStyle = "#FFFFFF";
          ctx.fill();
          ctx.restore();
        }

        // Render Spark Particles
        particlesRef.current.forEach((p, idx) => {
          p.x += p.vx;
          p.y += p.vy;
          p.life -= p.decay;

          if (p.life <= 0) {
            particlesRef.current.splice(idx, 1);
            return;
          }

          ctx.save();
          ctx.beginPath();
          ctx.arc(p.x, p.y, Math.max(0, p.size * p.life), 0, Math.PI * 2);
          ctx.fillStyle = p.color;
          ctx.globalAlpha = p.life;
          ctx.shadowBlur = 10;
          ctx.shadowColor = p.color;
          ctx.fill();
          ctx.restore();
        });
      } else if (mode === "dots") {
        // Render Group of Dots Swarm
        particlesRef.current.forEach((p, idx) => {
          p.x += p.vx;
          p.y += p.vy;
          p.life -= p.decay;

          if (p.life <= 0) {
            particlesRef.current.splice(idx, 1);
            return;
          }

          ctx.save();
          ctx.beginPath();
          ctx.arc(p.x, p.y, Math.max(0, p.size * p.life), 0, Math.PI * 2);
          ctx.fillStyle = p.color;
          ctx.globalAlpha = p.life * 0.85;
          ctx.shadowBlur = 12;
          ctx.shadowColor = p.color;
          ctx.fill();
          ctx.restore();
        });

        // Glowing center dot at mouse
        if (mx > 0 && my > 0) {
          ctx.save();
          ctx.beginPath();
          ctx.arc(mx, my, 6, 0, Math.PI * 2);
          ctx.fillStyle = "#00C853";
          ctx.shadowBlur = 15;
          ctx.shadowColor = "#00C853";
          ctx.fill();
          ctx.restore();
        }
      } else if (mode === "matrix") {
        // Draw Node Connections to Mouse and Neighboring Nodes
        particlesRef.current.forEach((p, idx) => {
          p.x += p.vx;
          p.y += p.vy;
          p.life -= p.decay;

          if (p.life <= 0) {
            particlesRef.current.splice(idx, 1);
            return;
          }

          const dx = mx - p.x;
          const dy = my - p.y;
          const dist = Math.sqrt(dx * dx + dy * dy);

          if (dist < 180) {
            ctx.beginPath();
            ctx.moveTo(p.x, p.y);
            ctx.lineTo(mx, my);
            ctx.strokeStyle = `rgba(0, 200, 83, ${(1 - dist / 180) * p.life * 0.6})`;
            ctx.lineWidth = 1;
            ctx.stroke();
          }

          ctx.save();
          ctx.beginPath();
          ctx.arc(p.x, p.y, Math.max(0, p.size), 0, Math.PI * 2);
          ctx.fillStyle = p.color;
          ctx.globalAlpha = p.life;
          ctx.shadowBlur = 8;
          ctx.shadowColor = "#00C853";
          ctx.fill();
          ctx.restore();
        });
      }

      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      window.removeEventListener("resize", resizeCanvas);
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("touchmove", handleTouchMove);
      cancelAnimationFrame(animationFrameId);
    };
  }, [mode]);

  return (
    <>
      {/* Non-interactive Overlay Canvas */}
      <canvas
        ref={canvasRef}
        style={{
          position: "fixed",
          top: 0,
          left: 0,
          width: "100vw",
          height: "100vh",
          pointerEvents: "none",
          zIndex: 9999,
        }}
      />

      {/* Floating Mode Controls Glass Widget */}
      <Paper
        elevation={6}
        sx={{
          position: "fixed",
          bottom: 24,
          right: 24,
          zIndex: 10000,
          bgcolor: "rgba(15, 23, 42, 0.85)",
          backdropFilter: "blur(10px)",
          border: "1px solid rgba(0, 200, 83, 0.3)",
          borderRadius: 4,
          p: 0.75,
          boxShadow: "0 8px 32px rgba(0,0,0,0.5), 0 0 15px rgba(0,200,83,0.15)",
        }}
      >
        <Stack direction="row" spacing={0.5} alignItems="center">
          <Tooltip title="Electric Thunder Mode (⚡)" arrow placement="top">
            <IconButton
              size="small"
              onClick={() => setMode("thunder")}
              sx={{
                color: mode === "thunder" ? "#00E5FF" : "#94A3B8",
                bgcolor: mode === "thunder" ? "rgba(0, 229, 255, 0.15)" : "transparent",
                border: mode === "thunder" ? "1px solid #00E5FF" : "1px solid transparent",
                "&:hover": { bgcolor: "rgba(0, 229, 255, 0.2)" },
              }}
            >
              <FlashOnIcon fontSize="small" />
            </IconButton>
          </Tooltip>

          <Tooltip title="Group of Dots Swarm Mode (✨)" arrow placement="top">
            <IconButton
              size="small"
              onClick={() => setMode("dots")}
              sx={{
                color: mode === "dots" ? "#00C853" : "#94A3B8",
                bgcolor: mode === "dots" ? "rgba(0, 200, 83, 0.15)" : "transparent",
                border: mode === "dots" ? "1px solid #00C853" : "1px solid transparent",
                "&:hover": { bgcolor: "rgba(0, 200, 83, 0.2)" },
              }}
            >
              <ScatterPlotIcon fontSize="small" />
            </IconButton>
          </Tooltip>

          <Tooltip title="Cyber Matrix Grid Mode (🌐)" arrow placement="top">
            <IconButton
              size="small"
              onClick={() => setMode("matrix")}
              sx={{
                color: mode === "matrix" ? "#7C4DFF" : "#94A3B8",
                bgcolor: mode === "matrix" ? "rgba(124, 77, 255, 0.15)" : "transparent",
                border: mode === "matrix" ? "1px solid #7C4DFF" : "1px solid transparent",
                "&:hover": { bgcolor: "rgba(124, 77, 255, 0.2)" },
              }}
            >
              <GrainIcon fontSize="small" />
            </IconButton>
          </Tooltip>

          <Tooltip title="Disable Cursor Effects (Off)" arrow placement="top">
            <IconButton
              size="small"
              onClick={() => setMode("off")}
              sx={{
                color: mode === "off" ? "#EF4444" : "#94A3B8",
                bgcolor: mode === "off" ? "rgba(239, 68, 68, 0.15)" : "transparent",
                border: mode === "off" ? "1px solid #EF4444" : "1px solid transparent",
                "&:hover": { bgcolor: "rgba(239, 68, 68, 0.2)" },
              }}
            >
              <OffIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      </Paper>
    </>
  );
}

export default CursorEffect;
