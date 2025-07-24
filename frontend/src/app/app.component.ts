import { Component } from "@angular/core";
import { TraceService } from "./services/trace.service";
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { Span } from "./interfaces/span";

@Component({
  selector: "app-root",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./app.component.html",
  styleUrls: [],
})
export class AppComponent {
  title = "Argus Trace Viewer";
  traceIdInput: string = ""; // Holds the value from the input box
  traceData: Span[] | null = null; // Will hold the JSON response
  isLoading: boolean = false;
  error: string | null = null;

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
        data.sort(
          (a, b) =>
            new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
        );

        this.traceData = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.error = `Failed to fetch trace. Status: ${err.status} - ${err.statusText}`;
        this.isLoading = false;
      },
    });
  }
}
