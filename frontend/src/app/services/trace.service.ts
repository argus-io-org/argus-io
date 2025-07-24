import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { Span } from "../interfaces/span";

@Injectable({
  providedIn: "root",
})
export class TraceService {
  private apiUrl = "http://localhost:8083/api/traces";

  constructor(private http: HttpClient) {}

  getTraceById(traceId: string): Observable<Span[]> {
    return this.http.get<Span[]>(`${this.apiUrl}/${traceId}`);
  }
}
