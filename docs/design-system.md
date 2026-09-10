# UMLPilot - Design System

## Brand Identity

**UMLPilot** es un editor UML colaborativo con IA. El diseño refleja profesionalismo, claridad y modernidad — herramienta de trabajo, no de entretenimiento.

## Color Palette

### Primary
| Token | Hex | Uso |
|-------|-----|-----|
| `blue-50` | `#eff6ff` | Backgrounds sutiles, hover states |
| `blue-100` | `#dbeafe` | Borders, backgrounds secundarios |
| `blue-200` | `#bfdbfe` | Borders activos |
| `blue-500` | `#3b82f6` | Primary actions, links, acentos |
| `blue-600` | `#2563eb` | Botones primary, hover |
| `blue-700` | `#1d4ed8` | Botones primary active |
| `blue-900` | `#1e3a5f` | Headers, dark backgrounds |

### Neutrals
| Token | Hex | Uso |
|-------|-----|-----|
| `white` | `#ffffff` | Backgrounds principales |
| `gray-50` | `#f9fafb` | Backgrounds secundarios, sidebars |
| `gray-100` | `#f3f4f6` | Cards, inputs background |
| `gray-200` | `#e5e7eb` | Borders, dividers |
| `gray-300` | `#d1d5db` | Borders inactivos |
| `gray-400` | `#9ca3af` | Placeholders, icons secundarios |
| `gray-500` | `#6b7280` | Texto secundario |
| `gray-600` | `#4b5563` | Texto moyen |
| `gray-700` | `#374151` | Texto primary |
| `gray-800` | `#1f2937` | Texto headings |
| `gray-900` | `#111827` | Texto darkest |

### Semantic
| Token | Hex | Uso |
|-------|-----|-----|
| `green-500` | `#22c55e` | Success, online, connect |
| `green-600` | `#16a34a` | Success hover |
| `red-500` | `#ef4444` | Error, danger, delete |
| `red-600` | `#dc2626` | Error hover, danger actions |
| `yellow-500` | `#eab308` | Warning, locked |
| `orange-500` | `#f97316` | Warning secundario |

## Typography

### Font Family
- **Primary**: `Inter, system-ui, -apple-system, sans-serif`
- **Mono (UML canvas)**: `JetBrains Mono, ui-monospace, Consolas, monospace`

### Type Scale
| Token | Size | Weight | Line Height | Uso |
|-------|------|--------|-------------|-----|
| `text-xs` | 12px | 400 | 16px | Labels, captions |
| `text-sm` | 14px | 400 | 20px | Body secondary |
| `text-base` | 16px | 400 | 24px | Body primary |
| `text-lg` | 18px | 500 | 28px | Subheadings |
| `text-xl` | 20px | 600 | 28px | Section titles |
| `text-2xl` | 24px | 700 | 32px | Page titles |
| `text-3xl` | 30px | 700 | 36px | Hero headings |

## Spacing

Sistema de 4px base:
| Token | Value |
|-------|-------|
| `space-1` | 4px |
| `space-2` | 8px |
| `space-3` | 12px |
| `space-4` | 16px |
| `space-5` | 20px |
| `space-6` | 24px |
| `space-8` | 32px |
| `space-10` | 40px |
| `space-12` | 48px |

## Border Radius

| Token | Value | Uso |
|-------|-------|-----|
| `rounded-sm` | 4px | Inputs, badges |
| `rounded` | 6px | Buttons, cards pequeños |
| `rounded-lg` | 8px | Cards, modals |
| `rounded-xl` | 12px | Cards grandes, secciones |
| `rounded-2xl` | 16px | Featured cards |
| `rounded-full` | 9999px | Avatars, pills |

## Shadows

| Token | Value | Uso |
|-------|-------|-----|
| `shadow-sm` | `0 1px 2px rgba(0,0,0,0.05)` | Inputs, cards sutiles |
| `shadow` | `0 1px 3px rgba(0,0,0,0.1), 0 1px 2px rgba(0,0,0,0.06)` | Cards normales |
| `shadow-md` | `0 4px 6px rgba(0,0,0,0.07), 0 2px 4px rgba(0,0,0,0.06)` | Dropdowns, popovers |
| `shadow-lg` | `0 10px 15px rgba(0,0,0,0.1), 0 4px 6px rgba(0,0,0,0.05)` | Modals |
| `shadow-xl` | `0 20px 25px rgba(0,0,0,0.1), 0 10px 10px rgba(0,0,0,0.04)` | Tooltips |

## Componentes

### Buttons
| Variant | Classes |
|---------|---------|
| Primary | `bg-blue-600 hover:bg-blue-700 text-white rounded-lg px-4 py-2 font-medium transition-colors` |
| Secondary | `bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg px-4 py-2 font-medium transition-colors` |
| Danger | `bg-red-600 hover:bg-red-700 text-white rounded-lg px-4 py-2 font-medium transition-colors` |
| Ghost | `text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg px-3 py-2 text-sm transition-colors` |

### Inputs
| State | Classes |
|-------|---------|
| Default | `w-full rounded-lg border border-gray-300 px-3 py-2 text-gray-900 focus:border-blue-500 focus:ring-2 focus:ring-blue-500 outline-none transition-colors` |
| Error | `border-red-500 focus:border-red-500 focus:ring-red-500` |
| Disabled | `bg-gray-100 text-gray-400 cursor-not-allowed` |

### Cards
| Type | Classes |
|------|---------|
| Default | `rounded-xl bg-white border border-gray-200 p-5 shadow-sm` |
| Interactive | `rounded-xl bg-white border border-gray-200 p-5 shadow-sm hover:border-blue-300 hover:shadow-md transition-all cursor-pointer` |
| Selected | `rounded-xl bg-blue-50 border-2 border-blue-500 p-5 shadow-sm` |

## Layout

### Page Structure
```
┌──────────────────────────────────────┐
│ Header (bg-blue-900, h-12)           │
├──────────────────────────────────────┤
│ Toolbar (bg-gray-50, h-10)           │
├────────────────────────┬─────────────┤
│                        │  Properties │
│     Canvas (flex-1)    │  Panel      │
│                        │  (w-72)     │
│                        │             │
└────────────────────────┴─────────────┘
```

### Breakpoints
| Name | Width | Uso |
|------|-------|-----|
| `sm` | 640px | Mobile landscape |
| `md` | 768px | Tablet |
| `lg` | 1024px | Desktop |
| `xl` | 1280px | Desktop grande |

## Iconografía

**Librería**: Lucide React
**Estilo**: Stroke, 1.5px, 24x24px default
**Colores**: Hereda del texto padre (text-gray-600, text-blue-600, etc.)

| Icono | Componente | Uso |
|-------|-----------|-----|
| Plus | `<Plus />` | Añadir elemento |
| Trash | `<Trash />` | Eliminar |
| Lock/Unlock | `<Lock />` / `<LockOpen />` | Bloquear/desbloquear diagrama |
| ChevronLeft | `<ChevronLeft />` | Navegación atrás |
| X | `<X />` | Cerrar, eliminar item |
| Eye | `<Eye />` | Visibilidad pública |
| FileText | `<FileText />` | Documento/nota |

## UML Canvas

### Element Colors
| Elemento | Background | Border | Text |
|----------|-----------|--------|------|
| Clase | `white` | `gray-800` | `gray-900` |
| Interfaz | `white` | `gray-800` | `gray-900` |
| Enumeración | `white` | `gray-800` | `gray-900` |
| Nota | `yellow-50` | `yellow-200` | `gray-700` |

### Relationship Styles
| Tipo | Stroke | Dash | Marker |
|------|--------|------|--------|
| Asociación | `gray-800`, 2px | sólida | flecha llena |
| Herencia | `gray-800`, 2px | sólida | triángulo vacío |
| Dependencia | `gray-800`, 1.5px | punteada `5,5` | flecha abierta |
| Agregación | `gray-800`, 2px | sólida | diamante vacío |
| Composición | `gray-800`, 2px | sólida | diamante lleno |
| Realización | `gray-800`, 1.5px | punteada | flecha abierta |

## Accessibility

- **Contraste mínimo**: 4.5:1 para texto, 3:1 para UI elements
- **Focus rings**: `focus:ring-2 focus:ring-blue-500 focus:ring-offset-2`
- **Touch targets**: Mínimo 44x44px para elementos interactivos
- **Keyboard navigation**: Tab order lógico, Escape para cerrar modales
- **Screen reader**: aria-labels en botones icon-only
