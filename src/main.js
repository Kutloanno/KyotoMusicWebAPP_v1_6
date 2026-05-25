document.addEventListener('DOMContentLoaded', () => {
  // theme switcher
  const themeToggleBtn = document.getElementById('theme-toggle');
  const savedTheme = localStorage.getItem('theme');
  if (savedTheme === 'light') {
    document.body.classList.add('light-theme');
  }
  
  if (themeToggleBtn) {
    themeToggleBtn.addEventListener('click', () => {
      document.body.classList.toggle('light-theme');
      const isLight = document.body.classList.contains('light-theme');
      localStorage.setItem('theme', isLight ? 'light' : 'dark');
    });
  }

  // fret modal
  const fretTrigger = document.getElementById('fret-trigger');
  const fretModal = document.getElementById('fret-modal');
  const modalCloseBtn = document.getElementById('modal-close');
  
  if (fretTrigger && fretModal) {
    fretTrigger.addEventListener('click', (e) => {
      e.preventDefault();
      fretModal.classList.add('active');
      document.body.style.overflow = 'hidden'; // lock bg scroll
    });
  }
  
  if (modalCloseBtn && fretModal) {
    modalCloseBtn.addEventListener('click', (e) => {
      e.preventDefault();
      fretModal.classList.remove('active');
      document.body.style.overflow = '';
    });
  }

  // kyoto modal
  const kyotoTrigger = document.getElementById('kyoto-trigger');
  const kyotoModal = document.getElementById('kyoto-modal');
  const kyotoCloseBtn = document.getElementById('kyoto-modal-close');
  
  if (kyotoTrigger && kyotoModal) {
    kyotoTrigger.addEventListener('click', (e) => {
      e.preventDefault();
      kyotoModal.classList.add('active');
      document.body.style.overflow = 'hidden'; // lock bg scroll
    });
  }
  
  if (kyotoCloseBtn && kyotoModal) {
    kyotoCloseBtn.addEventListener('click', (e) => {
      e.preventDefault();
      kyotoModal.classList.remove('active');
      document.body.style.overflow = '';
    });
  }
});