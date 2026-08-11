import { useEffect, useState } from "react";
import {
  Box,
  Typography,
  Paper,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  CircularProgress,
  TextField,
  Stack,
  Pagination,
} from "@mui/material";

import { getAuditLogs } from "../../services/auditLogService";

function AuditLogs() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    loadLogs();
  }, [search, page]);

  const loadLogs = async () => {
    setLoading(true);
    try {
      const data = await getAuditLogs(search, page - 1, 15);
      setLogs(data.content || []);
      setTotalPages(data.totalPages || 1);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box p={3}>
      <Typography variant="h4" fontWeight={700} mb={3}>
        Platform Administrative Audit Logs
      </Typography>

      {/* Filter Bar */}
      <Paper sx={{ p: 3, mb: 3, borderRadius: 4 }}>
        <TextField
          label="Search Audit Trails by user, action, IP, details..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          fullWidth
          size="small"
        />
      </Paper>

      {loading ? (
        <Box sx={{ display: "flex", justifyContent: "center", mt: 8 }}>
          <CircularProgress />
        </Box>
      ) : (
        <>
          <Paper sx={{ overflow: "hidden", borderRadius: 4, mb: 2 }}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell><b>Timestamp</b></TableCell>
                  <TableCell><b>Action / Method Trigger</b></TableCell>
                  <TableCell><b>Operator Username</b></TableCell>
                  <TableCell><b>Client IP</b></TableCell>
                  <TableCell><b>Execution Details</b></TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {logs.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center">
                      No audit trails found.
                    </TableCell>
                  </TableRow>
                ) : (
                  logs.map((trail) => (
                    <TableRow key={trail.id} hover>
                      <TableCell sx={{ whiteSpace: "nowrap" }}>
                        {trail.timestamp ? new Date(trail.timestamp).toLocaleString() : "N/A"}
                      </TableCell>
                      <TableCell sx={{ fontWeight: "bold", color: "primary.main" }}>
                        {trail.action}
                      </TableCell>
                      <TableCell>{trail.username}</TableCell>
                      <TableCell sx={{ fontFamily: "monospace" }}>{trail.ipAddress}</TableCell>
                      <TableCell>{trail.details}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </Paper>

          <Box display="flex" justifyContent="center" mt={3}>
            <Pagination
              count={totalPages}
              page={page}
              onChange={(e, val) => setPage(val)}
              color="primary"
            />
          </Box>
        </>
      )}
    </Box>
  );
}

export default AuditLogs;
