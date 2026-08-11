import { createTheme } from "@mui/material/styles";

const getTheme = (mode) =>
  createTheme({
    palette: {
      mode,

      primary: {
        main: "#00C853",
      },

      secondary: {
        main: "#2979FF",
      },

      error: {
        main: "#FF5252",
      },

      warning: {
        main: "#FFC107",
      },

      success: {
        main: "#00C853",
      },

      background:
        mode === "dark"
          ? {
              default: "#0D1117",
              paper: "#161B22",
            }
          : {
              default: "#F5F7FA",
              paper: "#FFFFFF",
            },
    },

    shape: {
      borderRadius: 16,
    },

    typography: {
      fontFamily: `"Inter", "Roboto", sans-serif`,

      h3: {
        fontWeight: 700,
      },

      h4: {
        fontWeight: 700,
      },

      h5: {
        fontWeight: 600,
      },

      button: {
        textTransform: "none",
        fontWeight: 600,
      },
    },

    components: {

      MuiPaper: {
        styleOverrides: {
          root: {
            borderRadius: 18,
            transition: "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)",
            backgroundImage: "none",
          },
        },
      },

      MuiCard: {
        styleOverrides: {
          root: {
            borderRadius: 20,
            transition: "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)",
            position: "relative",
            "&:hover": {
              transform: "translateY(-4px) translateZ(10px)",
              boxShadow:
                mode === "dark"
                  ? "0 12px 30px -10px rgba(0, 200, 83, 0.2), 0 4px 20px rgba(0,0,0,0.5)"
                  : "0 12px 30px -10px rgba(0, 0, 0, 0.12), 0 4px 15px rgba(0,0,0,0.06)",
            },
          },
        },
      },

      MuiButton: {
        styleOverrides: {
          root: {
            borderRadius: 12,
            padding: "10px 22px",
            fontWeight: 600,
            transition: "all 0.25s cubic-bezier(0.4, 0, 0.2, 1)",
            "&:hover": {
              transform: "translateY(-2px)",
              boxShadow: "0 6px 20px rgba(0, 200, 83, 0.25)",
            },
            "&:active": {
              transform: "translateY(0px) scale(0.98)",
            },
          },
        },
      },

      MuiIconButton: {
        styleOverrides: {
          root: {
            transition: "all 0.25s ease",
            "&:hover": {
              transform: "scale(1.1) rotate(4deg)",
            },
            "&:active": {
              transform: "scale(0.95)",
            },
          },
        },
      },

      MuiChip: {
        styleOverrides: {
          root: {
            borderRadius: 10,
            fontWeight: 600,
            transition: "all 0.2s ease",
            "&:hover": {
              transform: "scale(1.05)",
              boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
            },
          },
        },
      },

      MuiTextField: {
        defaultProps: {
          variant: "outlined",
          size: "small",
        },
        styleOverrides: {
          root: {
            "& .MuiOutlinedInput-root": {
              borderRadius: 12,
              transition: "all 0.25s ease",
              "&:hover .MuiOutlinedInput-notchedOutline": {
                borderColor: mode === "dark" ? "#00C853" : "#00C853",
              },
              "&.Mui-focused": {
                boxShadow: "0 0 12px rgba(0, 200, 83, 0.3)",
              },
            },
          },
        },
      },

      MuiTableHead: {
        styleOverrides: {
          root: {
            backgroundColor:
              mode === "dark"
                ? "#1C2128"
                : "#F3F4F6",
          },
        },
      },

      MuiTableRow: {
        styleOverrides: {
          root: {
            transition: "all 0.2s ease",
            "&:hover": {
              backgroundColor:
                mode === "dark"
                  ? "rgba(0, 200, 83, 0.06) !important"
                  : "rgba(0, 200, 83, 0.04) !important",
            },
          },
        },
      },
    },
  });

export default getTheme;