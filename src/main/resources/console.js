document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.grid-action-form').forEach(form => {
        form.addEventListener('submit', event => {
            event.preventDefault();
            fetch(form.action, {
                method: 'POST',
                body: new URLSearchParams(new FormData(form))
            }).then(response => {
                if (!response.ok) throw new Error('Request failed: ' + response.status);
                location.reload();
            }).catch(err => alert('Action failed: ' + err.message));
        });
    });
});
