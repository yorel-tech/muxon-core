"use client";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface ConstraintEditorProps {
  constraints: any;
  onChange: (constraints: any) => void;
}

export default function ConstraintEditor({ constraints, onChange }: ConstraintEditorProps) {
  const updateConstraint = (key: string, value: any) => {
    onChange({ ...constraints, [key]: value });
  };

  return (
    <div className="space-y-4 border rounded-lg p-4">
      <div className="grid grid-cols-2 gap-4">
        <div>
          <Label htmlFor="min_iops">Minimum IOPS</Label>
          <Input
            id="min_iops"
            type="number"
            value={constraints.min_iops || ""}
            onChange={(e) => updateConstraint("min_iops", parseInt(e.target.value) || 0)}
            placeholder="10000"
          />
        </div>

        <div>
          <Label htmlFor="max_latency_ms">Max Latency (ms)</Label>
          <Input
            id="max_latency_ms"
            type="number"
            value={constraints.max_latency_ms || ""}
            onChange={(e) => updateConstraint("max_latency_ms", parseInt(e.target.value) || 0)}
            placeholder="10"
          />
        </div>
      </div>
    </div>
  );
}
