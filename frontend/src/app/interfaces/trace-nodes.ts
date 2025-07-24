import { Span } from "./span";

// This interface extends our original Span and adds the children array.
export interface TraceNode extends Span {
  children: TraceNode[];
  depth?: number; // Optional property for rendering
  leftOffsetPercent?: number; // Optional property for rendering
  widthPercent?: number; // Optional property for rendering
}
