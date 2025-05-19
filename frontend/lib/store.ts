import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { useEffect } from 'react';

interface UIState {
  isSidebarOpen: boolean;
  isSidebarCollapsed: boolean;
  toggleSidebar: () => void;
  toggleCollapse: () => void;
  setIsSidebarOpen: (isOpen: boolean) => void;
}

export const useUI = create<UIState>()(
  persist(
    (set) => ({
      isSidebarOpen: false,
      isSidebarCollapsed: false,
      toggleSidebar: () => set((s) => ({ isSidebarOpen: !s.isSidebarOpen })),
      toggleCollapse: () => set((s) => ({ isSidebarCollapsed: !s.isSidebarCollapsed })),
      setIsSidebarOpen: (isOpen: boolean) => set({ isSidebarOpen: isOpen })
    }),
    { name: 'ui' }
  )
);

export function useSidebarEffects(isAuthenticated: boolean) {
  const { isSidebarCollapsed, setIsSidebarOpen } = useUI();
  
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth >= 1024) {
        setIsSidebarOpen(false);
      }
    };
    
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [setIsSidebarOpen]);

  useEffect(() => {
    if (isAuthenticated) {
      document.documentElement.setAttribute(
        'data-sidebar-collapsed',
        isSidebarCollapsed ? 'true' : 'false'
      );
    }
  }, [isSidebarCollapsed, isAuthenticated]);
} 