export interface Span {
  spanId: string;
  traceId: string;
  parentSpanId: string | null;
  serviceName: string;
  methodName: string;
  startTime: string;
  endTime: string;
  durationMs: number;
  status: "SUCCESS" | "FAILED";
  errorMessage: string | null;
  tags: string;
}
