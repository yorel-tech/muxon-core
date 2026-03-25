"use client";

import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";

interface CapabilityEditorProps {
  capabilities: any;
  onChange: (capabilities: any) => void;
}

export default function CapabilityEditor({ capabilities, onChange }: CapabilityEditorProps) {
  const updateCapability = (key: string, value: any) => {
    onChange({ ...capabilities, [key]: value });
  };

  return (
    <div className="space-y-4 border rounded-lg p-4">
      <div className="grid grid-cols-2 gap-4">
        <div>
          <Label htmlFor="performance">Performance</Label>
          <Select
            value={capabilities.performance || "medium"}
            onValueChange={(value) => updateCapability("performance", value)}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="high">High</SelectItem>
              <SelectItem value="medium">Medium</SelectItem>
              <SelectItem value="low">Low</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div>
          <Label htmlFor="media">Media</Label>
          <Select
            value={capabilities.media || "any"}
            onValueChange={(value) => updateCapability("media", value)}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ssd">SSD</SelectItem>
              <SelectItem value="hdd">HDD</SelectItem>
              <SelectItem value="nvme">NVMe</SelectItem>
              <SelectItem value="any">Any</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div>
          <Label htmlFor="redundancy">Redundancy</Label>
          <Select
            value={capabilities.redundancy || "none"}
            onValueChange={(value) => updateCapability("redundancy", value)}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="replicated">Replicated</SelectItem>
              <SelectItem value="none">None</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div className="flex items-center space-x-2">
          <Switch
            id="shared"
            checked={capabilities.shared || false}
            onCheckedChange={(checked) => updateCapability("shared", checked)}
          />
          <Label htmlFor="shared">Shared Storage</Label>
        </div>
      </div>
    </div>
  );
}
