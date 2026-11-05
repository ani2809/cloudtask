
import { useCallback, useEffect, useState } from 'react'
import './App.css'

const getStoredValue = (key) =>
  localStorage.getItem(key) || sessionStorage.getItem(key)

const clearTokens = () => {
  ;['accessToken', 'refreshToken', 'tokenType', 'expiresIn'].forEach((key) => {
    localStorage.removeItem(key)
    sessionStorage.removeItem(key)
  })
}

const saveTokens = (data, storage = sessionStorage) => {
  storage.setItem('accessToken', data.accessToken)
  storage.setItem('refreshToken', data.refreshToken)
  storage.setItem('tokenType', data.tokenType || 'Bearer')
  storage.setItem('expiresIn', String(data.expiresIn))
}

async function parseResponse(response) {
  const text = await response.text()
  if (!text) return {}

  try {
    return JSON.parse(text)
  } catch {
    return { message: text }
  }
}

function getErrorMessage(data, fallback) {
  if (typeof data === 'string') return data
  if (data?.message) return data.message
  if (data?.detail) return data.detail
  if (data?.error) return data.error

  if (data?.errors) {
    if (Array.isArray(data.errors)) {
      return data.errors
        .map((item) => item.defaultMessage || item.message || item)
        .join(', ')
    }
    return Object.values(data.errors).join(', ')
  }

  return fallback
}

async function apiRequest(path, options = {}) {
  const makeRequest = async (token) => {
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers,
    }

    if (token) {
      headers.Authorization = `${getStoredValue('tokenType') || 'Bearer'} ${token}`
    }

    return fetch(path, { ...options, headers })
  }

  let response = await makeRequest(getStoredValue('accessToken'))

  if (response.status === 401 && path !== '/api/auth/refresh') {
    const refreshToken = getStoredValue('refreshToken')

    if (refreshToken) {
      const refreshResponse = await fetch('/api/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      })

      const refreshedData = await parseResponse(refreshResponse)

      if (refreshResponse.ok && refreshedData.accessToken) {
        const storage = localStorage.getItem('refreshToken')
          ? localStorage
          : sessionStorage

        saveTokens(refreshedData, storage)
        response = await makeRequest(refreshedData.accessToken)
      } else {
        clearTokens()
        throw new Error('Your session has expired. Please sign in again.')
      }
    }
  }

  const data = await parseResponse(response)

  if (!response.ok) {
    throw new Error(
      getErrorMessage(data, `Request failed (${response.status})`)
    )
  }

  return data
}

function App() {
  const [isLogin, setIsLogin] = useState(true)
  const [authenticated, setAuthenticated] = useState(
    Boolean(getStoredValue('accessToken'))
  )

  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(false)

  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const [tasks, setTasks] = useState([])
  const [loadingTasks, setLoadingTasks] = useState(false)
  const [taskError, setTaskError] = useState('')

  const [statusFilter, setStatusFilter] = useState('')
  const [priorityFilter, setPriorityFilter] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  const [showCreateForm, setShowCreateForm] = useState(false)
  const [taskTitle, setTaskTitle] = useState('')
  const [taskDescription, setTaskDescription] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [creatingTask, setCreatingTask] = useState(false)

  const [updatingTaskId, setUpdatingTaskId] = useState(null)

  const handleAuthSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)

    const endpoint = isLogin ? '/api/auth/login' : '/api/auth/register'
    const payload = isLogin
      ? { email, password }
      : { fullName, email, password }

    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })

      const data = await parseResponse(response)

      if (!response.ok) {
        throw new Error(
          getErrorMessage(data, `Request failed (${response.status})`)
        )
      }

      if (isLogin) {
        if (!data.accessToken || !data.refreshToken) {
          throw new Error('The server did not return the expected login tokens.')
        }

        clearTokens()
        saveTokens(data, rememberMe ? localStorage : sessionStorage)
        setAuthenticated(true)
        setPassword('')
      } else {
        setSuccess('Account created successfully! You can now sign in.')
        setIsLogin(true)
        setPassword('')
      }
    } catch (err) {
      setError(
        err instanceof TypeError
          ? 'Unable to connect to CloudTask. Check that the backend is running.'
          : err.message
      )
    } finally {
      setSubmitting(false)
    }
  }

  const loadTasks = useCallback(async () => {
    setLoadingTasks(true)
    setTaskError('')

    try {
      const params = new URLSearchParams({
        page: String(page),
        size: '10',
        sortBy: 'createdAt',
        sortDirection: 'DESC',
      })

      if (statusFilter) params.set('status', statusFilter)
      if (priorityFilter) params.set('priority', priorityFilter)

      const data = await apiRequest(`/api/tasks?${params.toString()}`)

      setTasks(data.content || [])
      setTotalPages(data.totalPages || 0)
      setTotalElements(data.totalElements || 0)
    } catch (err) {
      setTaskError(err.message)

      if (err.message.includes('session has expired')) {
        setAuthenticated(false)
      }
    } finally {
      setLoadingTasks(false)
    }
  }, [page, statusFilter, priorityFilter])

  useEffect(() => {
    if (!authenticated) return

    let cancelled = false

    const load = async () => {
      await Promise.resolve()

      if (!cancelled) {
        await loadTasks()
      }
    }

    void load()

    return () => {
      cancelled = true
    }
  }, [authenticated, loadTasks])

  const handleCreateTask = async (event) => {
    event.preventDefault()
    setCreatingTask(true)
    setTaskError('')

    try {
      await apiRequest('/api/tasks', {
        method: 'POST',
        body: JSON.stringify({
          title: taskTitle,
          description: taskDescription,
          dueDate: dueDate || null,
        }),
      })

      setTaskTitle('')
      setTaskDescription('')
      setDueDate('')
      setShowCreateForm(false)

      if (page !== 0) {
        setPage(0)
      } else {
        await loadTasks()
      }
    } catch (err) {
      setTaskError(err.message)
    } finally {
      setCreatingTask(false)
    }
  }

  const handleStatusChange = async (taskId, status) => {
    setUpdatingTaskId(taskId)
    setTaskError('')

    try {
      await apiRequest(`/api/tasks/${taskId}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      })

      await loadTasks()
    } catch (err) {
      setTaskError(err.message)
    } finally {
      setUpdatingTaskId(null)
    }
  }

  const handleLogout = async () => {
    const refreshToken = getStoredValue('refreshToken')

    try {
      if (refreshToken) {
        await fetch('/api/auth/logout', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        })
      }
    } finally {
      clearTokens()
      setAuthenticated(false)
      setTasks([])
      setError('')
      setSuccess('')
      setIsLogin(true)
    }
  }

  const switchMode = (loginMode) => {
    setIsLogin(loginMode)
    setError('')
    setSuccess('')
    setPassword('')
  }

  if (!authenticated) {
    return (
      <div className="app-container">
        <section className="brand-panel">
          <div className="brand">
            <div className="brand-icon">C</div>
            <span>CloudTask</span>
          </div>

          <div className="brand-content">
            <span className="eyebrow">WORK SMARTER. BUILD BETTER.</span>
            <h1>Bring your team's work together.</h1>
            <p>
              A simple, secure workspace to organize tasks, collaborate with
              your team, and keep every project moving forward.
            </p>
            <div className="feature-list">
              <div>✓ &nbsp; Organize and track tasks</div>
              <div>✓ &nbsp; Role-based access control</div>
              <div>✓ &nbsp; Secure JWT authentication</div>
            </div>
          </div>

          <div className="brand-footer">
            © 2026 CloudTask. Built for productive teams.
          </div>
        </section>

        <main className="auth-panel">
          <div className="auth-card">
            <div className="mobile-brand">
              <div className="brand-icon">C</div>
              <span>CloudTask</span>
            </div>

            <div className="auth-heading">
              <span className="eyebrow">WELCOME TO CLOUDTASK</span>
              <h2>{isLogin ? 'Welcome back' : 'Create your account'}</h2>
              <p>
                {isLogin
                  ? 'Sign in to continue to your workspace.'
                  : 'Get started by creating your CloudTask account.'}
              </p>
            </div>

            <div className="auth-tabs">
              <button
                type="button"
                className={isLogin ? 'active' : ''}
                onClick={() => switchMode(true)}
              >
                Sign In
              </button>
              <button
                type="button"
                className={!isLogin ? 'active' : ''}
                onClick={() => switchMode(false)}
              >
                Register
              </button>
            </div>

            {error && <div className="form-message error-message">{error}</div>}
            {success && <div className="form-message success-message">{success}</div>}

            <form onSubmit={handleAuthSubmit}>
              {!isLogin && (
                <div className="form-group">
                  <label htmlFor="fullName">Full name</label>
                  <input
                    id="fullName"
                    value={fullName}
                    onChange={(event) => setFullName(event.target.value)}
                    placeholder="Your full name"
                    maxLength={150}
                    required
                  />
                </div>
              )}

              <div className="form-group">
                <label htmlFor="email">Email address</label>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="you@example.com"
                  maxLength={320}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="password">Password</label>
                <input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="Enter your password"
                  minLength={8}
                  maxLength={72}
                  required
                />
              </div>

              {isLogin && (
                <div className="form-options">
                  <label className="remember-label">
                    <input
                      type="checkbox"
                      checked={rememberMe}
                      onChange={(event) => setRememberMe(event.target.checked)}
                    />
                    Remember me
                  </label>
                  <span className="forgot-password">Secure JWT login</span>
                </div>
              )}

              <button className="submit-button" type="submit" disabled={submitting}>
                {submitting
                  ? 'Please wait...'
                  : isLogin
                    ? 'Sign In'
                    : 'Create Account'}
                {!submitting && <span>→</span>}
              </button>
            </form>

            <p className="switch-text">
              {isLogin ? "Don't have an account?" : 'Already have an account?'}
              <button type="button" onClick={() => switchMode(!isLogin)}>
                {isLogin ? 'Register' : 'Sign in'}
              </button>
            </p>
          </div>
        </main>
      </div>
    )
  }

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div className="brand">
          <div className="brand-icon">C</div>
          <span>CloudTask</span>
        </div>
        <div className="header-actions">
          <span className="signed-in-label">Authenticated workspace</span>
          <button className="logout-button" onClick={handleLogout}>
            Sign out
          </button>
        </div>
      </header>

      <main className="dashboard-content">
        <div className="dashboard-title-row">
          <div>
            <span className="eyebrow">YOUR WORKSPACE</span>
            <h1>My Tasks</h1>
            <p>Organize your work and keep things moving.</p>
          </div>
          <button
            className="create-task-button"
            onClick={() => setShowCreateForm(!showCreateForm)}
          >
            {showCreateForm ? 'Cancel' : '+ Create Task'}
          </button>
        </div>

        {showCreateForm && (
          <form className="create-task-form" onSubmit={handleCreateTask}>
            <h2>Create a task</h2>

            <div className="form-group">
              <label htmlFor="taskTitle">Task title</label>
              <input
                id="taskTitle"
                value={taskTitle}
                onChange={(event) => setTaskTitle(event.target.value)}
                placeholder="e.g. Complete project documentation"
                maxLength={200}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="taskDescription">Description</label>
              <textarea
                id="taskDescription"
                value={taskDescription}
                onChange={(event) => setTaskDescription(event.target.value)}
                placeholder="Add task details..."
                rows={3}
              />
            </div>

            <div className="form-group">
              <label htmlFor="dueDate">Due date</label>
              <input
                id="dueDate"
                type="date"
                value={dueDate}
                onChange={(event) => setDueDate(event.target.value)}
              />
            </div>

            <button className="submit-button" type="submit" disabled={creatingTask}>
              {creatingTask ? 'Creating...' : 'Save Task'}
            </button>
          </form>
        )}

        <div className="task-summary">
          <div className="summary-card">
            <span>Total tasks</span>
            <strong>{totalElements}</strong>
          </div>
          <div className="summary-card">
            <span>Current page</span>
            <strong>{totalPages ? page + 1 : 0} / {totalPages}</strong>
          </div>
        </div>

        <div className="task-toolbar">
          <div>
            <label htmlFor="statusFilter">Status</label>
            <select
              id="statusFilter"
              value={statusFilter}
              onChange={(event) => {
                setStatusFilter(event.target.value)
                setPage(0)
              }}
            >
              <option value="">All statuses</option>
              <option value="TODO">To do</option>
              <option value="IN_PROGRESS">In progress</option>
              <option value="DONE">Done</option>
            </select>
          </div>

          <div>
            <label htmlFor="priorityFilter">Priority</label>
            <select
              id="priorityFilter"
              value={priorityFilter}
              onChange={(event) => {
                setPriorityFilter(event.target.value)
                setPage(0)
              }}
            >
              <option value="">All priorities</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
            </select>
          </div>
        </div>

        {taskError && (
          <div className="form-message error-message">{taskError}</div>
        )}

        {loadingTasks ? (
          <p className="empty-state">Loading your tasks...</p>
        ) : tasks.length === 0 ? (
          <div className="empty-state">
            <h2>No tasks found</h2>
            <p>Create your first task or change the filters.</p>
          </div>
        ) : (
          <div className="task-list">
            {tasks.map((task) => (
              <article className="task-card" key={task.id}>
                <div className="task-card-main">
                  <div>
                    <h3>{task.title}</h3>
                    {task.description && <p>{task.description}</p>}
                    <div className="task-meta">
                      <span className={`priority priority-${(task.priority || 'medium').toLowerCase()}`}>
                        {task.priority || 'MEDIUM'}
                      </span>
                      {task.dueDate && <span>Due: {task.dueDate}</span>}
                    </div>
                  </div>

                  <div className="task-status-control">
                    <label htmlFor={`status-${task.id}`}>Status</label>
                    <select
                      id={`status-${task.id}`}
                      value={task.status}
                      disabled={updatingTaskId === task.id}
                      onChange={(event) =>
                        handleStatusChange(task.id, event.target.value)
                      }
                    >
                      <option value="TODO">To do</option>
                      <option value="IN_PROGRESS">In progress</option>
                      <option value="DONE">Done</option>
                    </select>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}

        {totalPages > 1 && (
          <div className="pagination">
            <button
              disabled={page === 0 || loadingTasks}
              onClick={() => setPage((current) => current - 1)}
            >
              ← Previous
            </button>
            <span>Page {page + 1} of {totalPages}</span>
            <button
              disabled={page + 1 >= totalPages || loadingTasks}
              onClick={() => setPage((current) => current + 1)}
            >
              Next →
            </button>
          </div>
        )}
      </main>
    </div>
  )
}

export default App
