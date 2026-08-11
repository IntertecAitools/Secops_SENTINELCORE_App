import { useState, useEffect, useRef } from "react";
import { Box, Typography } from "@mui/material";
import RocketLaunchIcon from "@mui/icons-material/RocketLaunch";
import SecurityIcon from "@mui/icons-material/Security";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";

function RocketLaunchOverlay({ open, onComplete, userFullName }) {
  const [countdown, setCountdown] = useState(3);
  const [phase, setPhase] = useState("countdown"); // "countdown" | "launch"
  const canvasRef = useRef(null);
  const animationFrameId = useRef(null);

  // Particle system refs
  const particles = useRef([]);
  const stars = useRef([]);
  const rocketPos = useRef({ x: 0, y: 0, scale: 1, tilt: 0, opacity: 1 });

  useEffect(() => {
    if (!open) return;

    // Reset states
    setCountdown(3);
    setPhase("countdown");
    rocketPos.current = { x: 0, y: 0, scale: 1, tilt: -15, opacity: 1 };

    // Initialize 3D Stars canvas
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");

    const handleResize = () => {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    };
    handleResize();
    window.addEventListener("resize", handleResize);

    // Generate initial starfield
    const starList = [];
    for (let i = 0; i < 180; i++) {
      starList.push({
        x: (Math.random() - 0.5) * canvas.width * 2,
        y: (Math.random() - 0.5) * canvas.height * 2,
        z: Math.random() * canvas.width,
        size: Math.random() * 2 + 0.5,
      });
    }
    stars.current = starList;

    // Countdown interval (800ms per step: 3 ... 2 ... 1 ... 0)
    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          setPhase("launch");
          // Wait for rocket to fly completely off-screen before navigating to dashboard
          setTimeout(() => {
            if (onComplete) onComplete();
          }, 900);
          return 0;
        }
        return prev - 1;
      });
    }, 800);

    // Canvas render loop
    let startTime = Date.now();
    const render = () => {
      const elapsed = (Date.now() - startTime) / 1000;
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      const cx = canvas.width / 2;
      const cy = canvas.height / 2;

      // 1. Draw Starfield with 3D depth acceleration
      const speed = phase === "launch" ? 45 : 3;
      ctx.fillStyle = "#ffffff";

      stars.current.forEach((star) => {
        star.z -= speed;
        if (star.z <= 0) {
          star.z = canvas.width;
          star.x = (Math.random() - 0.5) * canvas.width * 2;
          star.y = (Math.random() - 0.5) * canvas.height * 2;
        }

        const k = 300 / star.z;
        const px = star.x * k + cx;
        const py = star.y * k + cy;

        if (px >= 0 && px <= canvas.width && py >= 0 && py <= canvas.height) {
          const alpha = Math.min(1, (1 - star.z / canvas.width) * 1.2);
          ctx.beginPath();
          ctx.arc(px, py, star.size * k * 0.8, 0, Math.PI * 2);
          ctx.fillStyle = `rgba(255, 255, 255, ${alpha})`;
          ctx.fill();

          // Draw warp tail if launching
          if (phase === "launch") {
            ctx.beginPath();
            ctx.moveTo(px, py);
            ctx.lineTo(px - (px - cx) * 0.2, py - (py - cy) * 0.2);
            ctx.strokeStyle = `rgba(0, 200, 83, ${alpha * 0.5})`;
            ctx.lineWidth = star.size * k * 0.6;
            ctx.stroke();
          }
        }
      });

      // 2. Thruster Flame & Smoke Particles
      const rocketCenterX = cx + rocketPos.current.x;
      const rocketCenterY = cy + rocketPos.current.y + 110 * rocketPos.current.scale;

      // Spawn thruster particles
      const spawnCount = phase === "launch" ? 20 : 4;
      for (let i = 0; i < spawnCount; i++) {
        particles.current.push({
          x: rocketCenterX + (Math.random() - 0.5) * 20 * rocketPos.current.scale,
          y: rocketCenterY,
          vx: (Math.random() - 0.5) * 5,
          vy: Math.random() * 8 + (phase === "launch" ? 20 : 4),
          size: Math.random() * 12 + 6,
          life: 1,
          decay: Math.random() * 0.04 + 0.02,
          type: Math.random() > 0.3 ? "flame" : "smoke",
        });
      }

      // Render & update particles
      particles.current.forEach((p, idx) => {
        p.x += p.vx;
        p.y += p.vy;
        p.life -= p.decay;
        p.size += 0.4;

        if (p.life <= 0) {
          particles.current.splice(idx, 1);
          return;
        }

        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size * rocketPos.current.scale, 0, Math.PI * 2);
        if (p.type === "flame") {
          const color =
            p.life > 0.7
              ? `rgba(255, 255, 255, ${p.life})`
              : p.life > 0.4
              ? `rgba(0, 200, 83, ${p.life})`
              : `rgba(255, 171, 0, ${p.life * 0.8})`;
          ctx.fillStyle = color;
          ctx.shadowBlur = 15;
          ctx.shadowColor = "#00C853";
        } else {
          ctx.fillStyle = `rgba(30, 41, 59, ${p.life * 0.4})`;
          ctx.shadowBlur = 0;
        }
        ctx.fill();
      });

      // 3. Animate Rocket position during Launch phase (Launches all the way off-screen)
      if (phase === "launch") {
        rocketPos.current.y -= 36; // Rapid upward launch trajectory
        rocketPos.current.x += 24; // Move right towards top-right screen edge
        rocketPos.current.scale += 0.02; // Trajectory scale
        rocketPos.current.tilt = Math.min(50, rocketPos.current.tilt + 3.5);
      } else {
        // Hovering rumble during 3-2-1 countdown
        rocketPos.current.y = Math.sin(elapsed * 8) * 6;
        rocketPos.current.tilt = -15 + Math.sin(elapsed * 4) * 3;
      }

      animationFrameId.current = requestAnimationFrame(render);
    };

    render();

    return () => {
      clearInterval(timer);
      window.removeEventListener("resize", handleResize);
      if (animationFrameId.current) {
        cancelAnimationFrame(animationFrameId.current);
      }
    };
  }, [open, phase]);

  if (!open) return null;

  return (
    <Box
      sx={{
        position: "fixed",
        inset: 0,
        zIndex: 99999,
        bgcolor: "#030712",
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
        alignItems: "center",
        overflow: "hidden",
        userSelect: "none",
      }}
    >
      {/* Background 3D Canvas */}
      <canvas
        ref={canvasRef}
        style={{
          position: "absolute",
          top: 0,
          left: 0,
          width: "100%",
          height: "100%",
          pointerEvents: "none",
        }}
      />

      {/* Cyber Grid Background Overlay */}
      <Box
        sx={{
          position: "absolute",
          inset: 0,
          backgroundImage:
            "linear-gradient(rgba(0, 200, 83, 0.07) 1px, transparent 1px), linear-gradient(90deg, rgba(0, 200, 83, 0.07) 1px, transparent 1px)",
          backgroundSize: "40px 40px",
          pointerEvents: "none",
          maskImage: "radial-gradient(circle at center, black 30%, transparent 80%)",
        }}
      />

      {/* Top Telemetry & Clearance Header */}
      <Box
        sx={{
          position: "absolute",
          top: 40,
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: 1,
          zIndex: 10,
        }}
      >
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 1.5,
            px: 3,
            py: 1,
            borderRadius: 50,
            bgcolor: "rgba(0, 200, 83, 0.1)",
            border: "1px solid rgba(0, 200, 83, 0.4)",
            boxShadow: "0 0 25px rgba(0, 200, 83, 0.25)",
            backdropFilter: "blur(10px)",
          }}
        >
          <CheckCircleIcon sx={{ color: "#00C853", fontSize: 22 }} />
          <Typography
            variant="subtitle2"
            sx={{
              color: "#00C853",
              fontWeight: 800,
              letterSpacing: 2,
              fontFamily: "monospace",
              textTransform: "uppercase",
            }}
          >
            AUTHENTICATION SUCCESSFUL • ACCESS GRANTED
          </Typography>
        </Box>

        {userFullName && (
          <Typography
            variant="h6"
            sx={{
              color: "#F8FAFC",
              fontWeight: 700,
              mt: 0.5,
              textShadow: "0 0 20px rgba(255,255,255,0.4)",
            }}
          >
            Welcome, {userFullName}!
          </Typography>
        )}
      </Box>

      {/* 3D Rocket Component */}
      <Box
        sx={{
          position: "relative",
          zIndex: 5,
          transform: `translate(${rocketPos.current.x}px, ${rocketPos.current.y}px) scale(${rocketPos.current.scale}) rotate(${rocketPos.current.tilt}deg)`,
          transition: phase === "launch" ? "none" : "transform 0.1s ease-out",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          filter: "drop-shadow(0 0 35px rgba(0, 200, 83, 0.5))",
        }}
      >
        {/* SVG 3D Rocket Mesh */}
        <svg width="180" height="260" viewBox="0 0 180 260" fill="none">
          <defs>
            {/* Metallic Body Gradient */}
            <linearGradient id="rocketBodyGrad" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#1E293B" />
              <stop offset="40%" stopColor="#475569" />
              <stop offset="70%" stopColor="#94A3B8" />
              <stop offset="100%" stopColor="#0F172A" />
            </linearGradient>

            {/* Glowing Nosecone Gradient */}
            <linearGradient id="noseGrad" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#00E676" />
              <stop offset="100%" stopColor="#00C853" />
            </linearGradient>

            {/* Fin Wing Gradient */}
            <linearGradient id="finGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#00C853" />
              <stop offset="100%" stopColor="#0F172A" />
            </linearGradient>

            {/* Thruster Flame Gradient */}
            <radialGradient id="flameGlow" cx="50%" cy="50%" r="50%">
              <stop offset="0%" stopColor="#FFFFFF" />
              <stop offset="40%" stopColor="#00E676" />
              <stop offset="80%" stopColor="#FFAB00" />
              <stop offset="100%" stopColor="transparent" />
            </radialGradient>
          </defs>

          {/* Left Wing Fin */}
          <path d="M45 150 L10 200 L45 185 Z" fill="url(#finGrad)" stroke="#00C853" strokeWidth="1.5" />

          {/* Right Wing Fin */}
          <path d="M135 150 L170 200 L135 185 Z" fill="url(#finGrad)" stroke="#00C853" strokeWidth="1.5" />

          {/* Main Fuselage Body */}
          <path
            d="M90 20 Q120 70 125 180 L55 180 Q60 70 90 20 Z"
            fill="url(#rocketBodyGrad)"
            stroke="#334155"
            strokeWidth="2"
          />

          {/* Nose Cap Cone */}
          <path d="M90 20 Q105 45 110 70 L70 70 Q75 45 90 20 Z" fill="url(#noseGrad)" />

          {/* Sentinel Security Emblem Badge */}
          <circle cx="90" cy="110" r="18" fill="#0F172A" stroke="#00C853" strokeWidth="2" />
          <path d="M90 100 L99 104 V112 Q90 120 90 120 Q90 120 81 112 V104 Z" fill="#00C853" />

          {/* Glass Cockpit Window */}
          <circle cx="90" cy="148" r="10" fill="#0284C7" stroke="#38BDF8" strokeWidth="2" />
          <circle cx="87" cy="145" r="3" fill="#E0F2FE" opacity="0.8" />

          {/* Exhaust Nozzles */}
          <rect x="68" y="180" width="14" height="15" rx="3" fill="#334155" />
          <rect x="98" y="180" width="14" height="15" rx="3" fill="#334155" />

          {/* Dynamic 3D Engine Flame Core */}
          <path
            d="M70 195 Q90 260 90 260 Q90 260 110 195 Z"
            fill="url(#flameGlow)"
            style={{
              transformOrigin: "90px 195px",
              animation: phase === "launch" ? "thrusterBlast 0.08s infinite alternate" : "thrusterIdle 0.15s infinite alternate",
            }}
          />
        </svg>

        {/* Thruster Ignition Style */}
        <style>{`
          @keyframes thrusterIdle {
            0% { transform: scaleY(0.9) scaleX(0.95); opacity: 0.8; }
            100% { transform: scaleY(1.15) scaleX(1.05); opacity: 1; }
          }
          @keyframes thrusterBlast {
            0% { transform: scaleY(1.8) scaleX(1.3); opacity: 0.95; }
            100% { transform: scaleY(2.6) scaleX(1.6); opacity: 1; filter: drop-shadow(0 0 20px #00E676); }
          }
        `}</style>
      </Box>

      {/* Central Holographic 3D Countdown Ring */}
      <Box
        sx={{
          position: "absolute",
          bottom: 100,
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          zIndex: 10,
        }}
      >
        <Box
          sx={{
            position: "relative",
            width: 110,
            height: 110,
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            borderRadius: "50%",
            background: "rgba(15, 23, 42, 0.6)",
            border: "2px solid rgba(0, 200, 83, 0.4)",
            boxShadow: "0 0 40px rgba(0, 200, 83, 0.3), inset 0 0 20px rgba(0, 200, 83, 0.2)",
            backdropFilter: "blur(12px)",
          }}
        >
          {/* Animated Countdown Progress Circle */}
          <svg width="110" height="110" style={{ position: "absolute", transform: "rotate(-90deg)" }}>
            <circle cx="55" cy="55" r="48" stroke="rgba(255,255,255,0.1)" strokeWidth="4" fill="none" />
            <circle
              cx="55"
              cy="55"
              r="48"
              stroke="#00C853"
              strokeWidth="4"
              fill="none"
              strokeDasharray="301.59"
              strokeDashoffset={(301.59 * countdown) / 3}
              style={{ transition: "stroke-dashoffset 1s linear" }}
            />
          </svg>

          {/* Countdown Display Number */}
          <Typography
            variant="h2"
            sx={{
              fontFamily: "monospace",
              fontWeight: 900,
              color: phase === "launch" ? "#00E676" : "#F8FAFC",
              textShadow: phase === "launch" ? "0 0 25px #00E676" : "0 0 15px rgba(255,255,255,0.6)",
              animation: "pulseScale 0.6s ease-out infinite alternate",
            }}
          >
            {phase === "launch" ? "🚀" : `0${countdown}`}
          </Typography>
        </Box>

        {/* Action Status Label */}
        <Typography
          variant="h6"
          sx={{
            mt: 2.5,
            color: "#00C853",
            fontFamily: "monospace",
            fontWeight: 800,
            letterSpacing: 3,
            textShadow: "0 0 12px rgba(0, 200, 83, 0.6)",
          }}
        >
          {phase === "launch" ? "INITIALIZING WARP DRIVE..." : "COUNTDOWN TO LAUNCH"}
        </Typography>

        <Typography
          variant="caption"
          sx={{
            mt: 0.5,
            color: "#94A3B8",
            fontFamily: "monospace",
            letterSpacing: 1.5,
          }}
        >
          REDIRECTING TO SECURITY PORTAL IN 3 SECONDS
        </Typography>
      </Box>

      {/* Pulse Keyframe Animation */}
      <style>{`
        @keyframes pulseScale {
          0% { transform: scale(1); }
          100% { transform: scale(1.08); }
        }
      `}</style>
    </Box>
  );
}

export default RocketLaunchOverlay;
