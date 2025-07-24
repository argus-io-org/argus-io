import { Component } from "@angular/core";
import { TraceService } from "./services/trace.service";
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { Span } from "./interfaces/span";
import { TraceNode } from "./interfaces/trace-nodes";

@Component({
  selector: "app-root",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.css"],
})
export class AppComponent {
  title = "Argus Trace Viewer";
  traceIdInput: string = ""; // Holds the value from the input box
  traceData: Span[] | null = null; // Will hold the JSON response
  isLoading: boolean = false;
  error: string | null = null;

  traceTree: TraceNode[] = []; // Will hold the structured, calculated data for the chart
  totalDuration: number = 0;

  // Inject our TraceService
  constructor(private traceService: TraceService) {}

  onSearch(): void {
    if (!this.traceIdInput.trim()) {
      return; // Don't search if the input is empty
    }

    this.isLoading = true;
    this.traceData = null;
    this.error = null;

    this.traceService.getTraceById(this.traceIdInput).subscribe({
      next: (data) => {
        /*
        data.sort(
          (a, b) =>
            new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
        );
        */

        this.traceData = data;
        this.buildTraceTree(data);

        this.isLoading = false;
      },
      error: (err) => {
        this.error = `Failed to fetch trace. Status: ${err.status} - ${err.statusText}`;
        this.isLoading = false;
      },
    });
  }

  // In AppComponent.ts

  private buildTraceTree(spans: Span[]): void {
    if (spans.length === 0) {
      this.traceTree = [];
      this.totalDuration = 0;
      return;
    }

    spans.sort(
      (a, b) =>
        new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
    );

    const traceStartTime = new Date(spans[0].startTime).getTime();
    const traceEndTime = Math.max(
      ...spans.map((s) => new Date(s.endTime).getTime())
    );
    this.totalDuration = traceEndTime - traceStartTime;

    // --- The Fix is Here ---
    // Explicitly type the Map and the children array.
    const spanMap = new Map<string, TraceNode>(
      spans.map((s) => [s.spanId, { ...s, children: [] as TraceNode[] }])
    );
    // --- End of Fix ---

    const rootSpans: TraceNode[] = [];

    for (const span of spanMap.values()) {
      if (span.parentSpanId && spanMap.has(span.parentSpanId)) {
        // Now TypeScript knows that .children is an array of TraceNode, so .push() is allowed.
        spanMap.get(span.parentSpanId)!.children.push(span);
      } else {
        rootSpans.push(span);
      }
    }

    // A recursive function to flatten the tree back into a list for rendering
    const flattenTree = (
      nodes: TraceNode[],
      depth: number,
      flatList: TraceNode[]
    ) => {
      for (const node of nodes) {
        const nodeStartTime = new Date(node.startTime).getTime();

        // Calculate visual properties
        node.depth = depth;
        node.leftOffsetPercent =
          ((nodeStartTime - traceStartTime) / this.totalDuration) * 100;
        node.widthPercent = (node.durationMs / this.totalDuration) * 100;

        flatList.push(node);

        node.children.sort(
          (a, b) =>
            new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
        );
        flattenTree(node.children, depth + 1, flatList);
      }
    };

    const finalFlatList: TraceNode[] = [];
    // Sort root spans by start time before processing
    rootSpans.sort(
      (a, b) =>
        new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
    );
    flattenTree(rootSpans, 0, finalFlatList);
    this.traceTree = finalFlatList;
  }
}
