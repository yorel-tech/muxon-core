# Muxon Cloud Management UI - Design Specification

## Overview
Modern, high-conversion Cloud Management UI for Infron (OSS) and Infron Nexus (Enterprise).

## Design Philosophy
- **Vibe Coding**: Prioritize visual quality, subtle animations, and polished interactions
- **Atomic Design**: Build from small, reusable components
- **Progressive Enhancement**: Server Components by default, client-side interactivity only where needed
- **Enterprise-First**: Design for both OSS and Enterprise, with conditional feature rendering

---

## Color Palette

### Primary Colors
```css
/* Primary Brand - Trustworthy Blue */
--color-primary-50: #eff6ff;
--color-primary-100: #dbeafe;
--color-primary-200: #bfdbfe;
--color-primary-300: #93c5fd;
--color-primary-400: #60a5fa;
--color-primary-500: #3b82f6;  /* Primary action color */
--color-primary-600: #2563eb;  /* Primary hover */
--color-primary-700: #1d4ed8;
--color-primary-800: #1e40af;
--color-primary-900: #1e3a8a;
```

### Semantic Colors
```css
/* Success - Green */
--color-success-50: #f0fdf4;
--color-success-500: #22c55e;
--color-success-600: #16a34a;

/* Warning - Amber */
--color-warning-50: #fefce8;
--color-warning-500: #f59e0b;
--color-warning-600: #d97706;

/* Error - Red */
--color-error-50: #fef2f2;
--color-error-500: #ef4444;
--color-error-600: #dc2626;

/* Info - Cyan */
--color-info-50: #ecfeff;
--color-info-500: #06b6d4;
--color-info-600: #0891b2;
```

### Neutral Colors
```css
/* Gray Scale */
--color-gray-50: #f9fafb;   /* Background light */
--color-gray-100: #f3f4f6;  /* Card background */
--color-gray-200: #e5e7eb;  /* Border */
--color-gray-300: #d1d5db;  /* Border hover */
--color-gray-400: #9ca3af;  /* Text muted */
--color-gray-500: #6b7280;  /* Text secondary */
--color-gray-600: #4b5563;  /* Text primary */
--color-gray-700: #374151;  /* Text dark */
--color-gray-800: #1f2937;  /* Heading */
--color-gray-900: #111827;  /* Background dark */
```

### Enterprise Accent (Nexus)
```css
/* Purple for Enterprise features */
--color-nexus-50: #faf5ff;
--color-nexus-500: #a855f7;
--color-nexus-600: #9333ea;
```

---

## Typography

### Font Family
```css
--font-sans: 'Inter', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
--font-mono: 'JetBrains Mono', 'Fira Code', 'SF Mono', monospace;
```

### Type Scale
```css
--text-xs: 0.75rem;    /* 12px - Labels, captions */
--text-sm: 0.875rem;   /* 14px - Body small, secondary */
--text-base: 1rem;     /* 16px - Body default */
--text-lg: 1.125rem;   /* 18px - Body large */
--text-xl: 1.25rem;    /* 20px - H4 */
--text-2xl: 1.5rem;    /* 24px - H3 */
--text-3xl: 1.875rem;  /* 30px - H2 */
--text-4xl: 2.25rem;   /* 36px - H1 */
--text-5xl: 3rem;      /* 48px - Display */
```

### Font Weights
```css
--font-normal: 400;
--font-medium: 500;
--font-semibold: 600;
--font-bold: 700;
```

### Line Heights
```css
--leading-tight: 1.25;
--leading-normal: 1.5;
--leading-relaxed: 1.75;
```

---

## Spacing System (8px Grid)

```css
--space-0: 0;
--space-1: 0.25rem;   /* 4px */
--space-2: 0.5rem;    /* 8px */
--space-3: 0.75rem;   /* 12px */
--space-4: 1rem;      /* 16px */
--space-5: 1.25rem;   /* 20px */
--space-6: 1.5rem;    /* 24px */
--space-8: 2rem;      /* 32px */
--space-10: 2.5rem;  /* 40px */
--space-12: 3rem;     /* 48px */
--space-16: 4rem;     /* 64px */
--space-20: 5rem;     /* 80px */
--space-24: 6rem;     /* 96px */
```

---

## Border Radius

```css
--radius-none: 0;
--radius-sm: 0.25rem;   /* 4px - Small elements */
--radius-md: 0.375rem;  /* 6px - Cards, buttons */
--radius-lg: 0.5rem;    /* 8px - Large cards */
--radius-xl: 0.75rem;   /* 12px - Modals */
--radius-full: 9999px; /* Pills, badges */
```

---

## Shadows

```css
--shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
--shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1);
--shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1);
--shadow-xl: 0 20px 25px -5px rgb(0 0 0 / 0.1);
--shadow-2xl: 0 25px 50px -12px rgb(0 0 0 / 0.25);
```

---

## Component Hierarchy

### Level 1: Atoms (Basic UI Elements)
```
components/ui/
├── button/
│   ├── button.tsx           # Primary, secondary, ghost, danger variants
│   └── button-group.tsx     # Button group component
├── input/
│   ├── input.tsx             # Text, email, password inputs
│   ├── textarea.tsx          # Multi-line text input
│   ├── select.tsx            # Dropdown select
│   ├── checkbox.tsx          # Checkbox with label
│   ├── radio.tsx             # Radio group
│   └── switch.tsx            # Toggle switch
├── feedback/
│   ├── badge.tsx              # Status badges, labels
│   ├── avatar.tsx             # User avatar with fallback
│   ├── progress.tsx           # Progress bars, spinners
│   ├── alert.tsx              # Info, success, warning, error
│   ├── toast.tsx              # Toast notifications
│   └── empty-state.tsx       # Empty state illustration
├── data-display/
│   ├── card.tsx               # Card container with variants
│   ├── table.tsx              # Data table with pagination
│   ├── list.tsx               # List items with actions
│   ├── stat-card.tsx           # Metric/stat card
│   └── status-indicator.tsx    # Online/offline status
└── navigation/
    ├── tabs.tsx                # Tab navigation
    ├── breadcrumb.tsx          # Breadcrumb trail
    └── pagination.tsx          # Pagination controls
```

### Level 2: Molecules (Composed Components)
```
components/
├── forms/
│   ├── search-input.tsx        # Search with filters
│   ├── form-field.tsx          # Input with label and error
│   └── action-bar.tsx         # Action buttons with filters
├── layout/
│   ├── sidebar.tsx             # Navigation sidebar
│   ├── header.tsx              # Top header with user menu
│   ├── page-header.tsx        # Page title with actions
│   └── content-area.tsx       # Main content wrapper
├── data/
│   ├── data-table.tsx           # Table with sort/filter/paginate
│   ├── resource-list.tsx       # List of resources
│   └── metric-grid.tsx         # Grid of stat cards
└── user/
    ├── user-menu.tsx           # User dropdown menu
    └── tenant-switcher.tsx     # Tenant switcher
```

### Level 3: Organisms (Complex Features)
```
components/
├── dashboard/
│   ├── system-dashboard.tsx     # System user dashboard
│   ├── tenant-dashboard.tsx    # Tenant user dashboard
│   └── onboarding.tsx         # First-time setup wizard
├── resources/
│   ├── datacenter-card.tsx     # Datacenter resource card
│   ├── node-card.tsx           # Node resource card
│   ├── vm-list.tsx             # VM list view
│   └── network-list.tsx        # Network list view
└── settings/
    ├── provider-form.tsx        # Provider configuration
    ├── idp-form.tsx            # Identity provider form
    └── user-form.tsx            # User management form
```

---

## Layout Structure

### App Shell
```
┌─────────────────────────────────────────────────────────────┐
│ Header (Logo, Breadcrumb, User Menu, Notifications)      │
├──────────┬──────────────────────────────────────────────────┤
│          │                                                 │
│ Sidebar  │  Main Content Area                               │
│ (Nav)    │  ┌─────────────────────────────────────────┐   │
│          │  │  Page Header (Title, Actions)          │   │
│          │  ├─────────────────────────────────────────┤   │
│          │  │                                         │   │
│          │  │  Dynamic Page Content                   │   │
│          │  │  - Cards, Tables, Forms                │   │
│          │  │                                         │   │
│          │  │                                         │   │
│          │  └─────────────────────────────────────────┘   │
│          │                                                 │
└──────────┴──────────────────────────────────────────────────┘
```

### Responsive Breakpoints
```css
--breakpoint-sm: 640px;   /* Mobile */
--breakpoint-md: 768px;   /* Tablet */
--breakpoint-lg: 1024px;  /* Desktop */
--breakpoint-xl: 1280px;  /* Wide desktop */
--breakpoint-2xl: 1536px; /* Extra wide */
```

---

## Animation System (Framer Motion)

### Duration
```css
--duration-fast: 150ms;
--duration-normal: 250ms;
--duration-slow: 350ms;
```

### Easing
```css
--ease-in: cubic-bezier(0.4, 0, 1, 1);
--ease-out: cubic-bezier(0, 0, 0.2, 1);
--ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);
```

### Animation Patterns
- **Fade In**: Entry animations for pages, modals
- **Slide**: Sidebar, dropdowns, drawers
- **Scale**: Buttons, cards on hover
- **Stagger**: List items, table rows

---

## User Type Detection

### System User
- Access: `/dashboard/system`
- Features: Providers, Datacenters, IDP, Users, Tenants, System Settings
- First-time: Onboarding wizard

### Tenant User
- Access: `/dashboard/tenant`
- Features: Tenant Users, Tenant Settings, Datacenters, VMs, Networks
- Dashboard: Usage metrics, limits, notifications

### Enterprise Detection
```typescript
// Detect enterprise features
const isEnterprise = user?.realm !== 'infron-dev' || 
                       user?.tenants?.length > 1;

// Conditional rendering
{isEnterprise && <EnterpriseFeatures />}
```

---

## Dark Mode Strategy

```css
:root {
  --bg-primary: #ffffff;
  --bg-secondary: #f9fafb;
  --text-primary: #111827;
  --text-secondary: #6b7280;
}

.dark {
  --bg-primary: #111827;
  --bg-secondary: #1f2937;
  --text-primary: #f9fafb;
  --text-secondary: #9ca3af;
}
```

---

## Accessibility

- **ARIA Labels**: All interactive elements
- **Keyboard Navigation**: Full keyboard support
- **Focus States**: Visible focus indicators
- **Color Contrast**: WCAG AA compliant
- **Screen Readers**: Semantic HTML structure

---

## Icon System

Use Lucide React for consistent iconography:
- `lucide-react` package
- 24px default size
- Consistent stroke width

---

## Next Steps

1. Install framer-motion
2. Set up Tailwind theme extension
3. Create atomic components
4. Build layout shell
5. Implement authentication context
6. Create user type detection
7. Build dashboards
8. Implement views
