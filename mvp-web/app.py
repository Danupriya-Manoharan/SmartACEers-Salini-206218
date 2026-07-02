#!/usr/bin/env python3
"""
ACE FlowSmith AI - Web Interface MVP
====================================
Flask-based web interface for hackathon demo.
Provides interactive AI-powered ACE application generation.
"""

from flask import Flask, render_template, request, jsonify, send_file
from flask_cors import CORS
import sys
import os
import json
import zipfile
from io import BytesIO
from datetime import datetime

# Add flowsmith module to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'flowsmith'))

# Import flowsmith functions
try:
    from flowsmith import (
        load_catalog, 
        find_pattern, 
        score_pattern,
        generate_application
    )
    FLOWSMITH_AVAILABLE = True
except ImportError:
    FLOWSMITH_AVAILABLE = False
    print("Warning: flowsmith module not found. Using mock data.")

app = Flask(__name__)
CORS(app)

# Configuration
REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GENERATED_DIR = os.path.join(REPO_ROOT, 'Generated')
os.makedirs(GENERATED_DIR, exist_ok=True)

# ============================================================================
# Routes
# ============================================================================

@app.route('/')
def index():
    """Main demo interface"""
    return render_template('index.html')

@app.route('/api/patterns')
def api_patterns():
    """Get all available patterns"""
    try:
        if FLOWSMITH_AVAILABLE:
            catalog = load_catalog()
            return jsonify({
                'success': True,
                'patterns': catalog['patterns'],
                'tokens': catalog['tokens'],
                'environments': catalog.get('environments', ['DEV', 'ACC', 'PRO'])
            })
        else:
            return jsonify({
                'success': False,
                'error': 'FlowSmith module not available'
            })
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/recommend', methods=['POST'])
def api_recommend():
    """AI-powered pattern recommendation"""
    try:
        data = request.json
        requirement = data.get('requirement', '')
        
        if not requirement:
            return jsonify({'success': False, 'error': 'Requirement is required'}), 400
        
        if FLOWSMITH_AVAILABLE:
            catalog = load_catalog()
            
            # Score all patterns
            scored = []
            for pattern in catalog['patterns']:
                score_result = score_pattern(pattern, requirement)
                scored.append({
                    'pattern': pattern,
                    'score': score_result['score'],
                    'hits': score_result['hits'],
                    'rationale': f"Matched keywords: {', '.join(score_result['hits'][:3])}" if score_result['hits'] else "No direct keyword matches"
                })
            
            # Sort by score
            scored.sort(key=lambda x: x['score'], reverse=True)
            
            # Return top 3
            return jsonify({
                'success': True,
                'recommendations': scored[:3],
                'engine': 'Keyword Matcher (watsonx.ai integration available in Java version)'
            })
        else:
            return jsonify({
                'success': False,
                'error': 'FlowSmith module not available'
            })
            
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/generate', methods=['POST'])
def api_generate():
    """Generate ACE application from pattern"""
    try:
        data = request.json
        pattern_id = data.get('pattern')
        subsys = data.get('subsys', '').upper().strip()
        app_name = data.get('app', '').strip()
        func_name = data.get('func', '').strip()
        ndm_name = data.get('ndm', '').upper().strip()
        
        # Validate inputs
        if not all([pattern_id, subsys, app_name, func_name]):
            return jsonify({
                'success': False,
                'error': 'Pattern, SUBSYS, APPNM, and FUNCNM are required'
            }), 400
        
        if FLOWSMITH_AVAILABLE:
            catalog = load_catalog()
            pattern = find_pattern(catalog, pattern_id)
            
            if not pattern:
                return jsonify({
                    'success': False,
                    'error': f'Pattern {pattern_id} not found'
                }), 404
            
            # Generate application
            result = generate_application(
                pattern=pattern,
                subsys=subsys,
                app=app_name,
                func=func_name,
                ndm=ndm_name,
                out_dir=GENERATED_DIR
            )
            
            # Get preview of key files
            preview_files = get_file_previews(result['output_dir'])
            
            return jsonify({
                'success': True,
                'project_name': result['project_name'],
                'output_dir': result['output_dir'],
                'files_generated': result['files_generated'],
                'preview': preview_files,
                'timestamp': datetime.now().isoformat()
            })
        else:
            return jsonify({
                'success': False,
                'error': 'FlowSmith module not available'
            })
            
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/download/<project_name>')
def api_download(project_name):
    """Download generated project as ZIP"""
    try:
        project_path = os.path.join(GENERATED_DIR, project_name)
        
        if not os.path.exists(project_path):
            return jsonify({'success': False, 'error': 'Project not found'}), 404
        
        # Create ZIP in memory
        memory_file = BytesIO()
        with zipfile.ZipFile(memory_file, 'w', zipfile.ZIP_DEFLATED) as zf:
            for root, dirs, files in os.walk(project_path):
                for file in files:
                    file_path = os.path.join(root, file)
                    arcname = os.path.relpath(file_path, GENERATED_DIR)
                    zf.write(file_path, arcname)
        
        memory_file.seek(0)
        return send_file(
            memory_file,
            mimetype='application/zip',
            as_attachment=True,
            download_name=f'{project_name}.zip'
        )
        
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# ============================================================================
# Helper Functions
# ============================================================================

def get_file_previews(project_dir, max_lines=50):
    """Get preview of key generated files"""
    previews = []
    
    # File extensions to preview
    preview_extensions = {
        '.msgflow': 'xml',
        '.esql': 'sql',
        '.prop': 'properties',
        '.properties': 'properties',
        '.project': 'xml',
        '.descriptor': 'xml'
    }
    
    try:
        for root, dirs, files in os.walk(project_dir):
            for file in files:
                ext = os.path.splitext(file)[1]
                if ext in preview_extensions:
                    file_path = os.path.join(root, file)
                    rel_path = os.path.relpath(file_path, project_dir)
                    
                    try:
                        with open(file_path, 'r', encoding='utf-8') as f:
                            lines = f.readlines()
                            content = ''.join(lines[:max_lines])
                            if len(lines) > max_lines:
                                content += f'\n... ({len(lines) - max_lines} more lines)'
                        
                        previews.append({
                            'path': rel_path,
                            'name': file,
                            'language': preview_extensions[ext],
                            'content': content,
                            'lines': len(lines)
                        })
                    except Exception as e:
                        print(f"Error reading {file_path}: {e}")
                        
    except Exception as e:
        print(f"Error getting previews: {e}")
    
    return previews

# ============================================================================
# Main
# ============================================================================

if __name__ == '__main__':
    print("=" * 70)
    print("🤖 ACE FlowSmith AI - Web Interface")
    print("=" * 70)
    print(f"FlowSmith Module: {'✓ Available' if FLOWSMITH_AVAILABLE else '✗ Not Available'}")
    print(f"Generated Files: {GENERATED_DIR}")
    print(f"Starting server at http://localhost:5000")
    print("=" * 70)
    
    app.run(debug=True, host='0.0.0.0', port=5000)

# Made with Bob
